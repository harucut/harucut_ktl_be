package com.harucut.media.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.media.dto.UserMediaDisplayNameUpdateRequest
import com.harucut.media.dto.UserMediaRegisterRequest
import com.harucut.media.dto.UserMediaResponse
import com.harucut.media.entity.UserMedia
import com.harucut.media.enums.UserMediaType
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.media.repository.UserMediaRepository
import com.harucut.storage.service.FileStorageService
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import com.harucut.util.response.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class UserMediaServiceImpl(
    private val userRepository: UserRepository,
    private val userMediaRepository: UserMediaRepository,
    private val fileStorageService: FileStorageService,
    private val subscriptionPolicy: MediaSubscriptionPolicy
) : UserMediaService {

    private val log = LoggerFactory.getLogger(javaClass)

    // 미디어 등록 (S3 key 정규화 + 멱등 처리, 사진/영상 분기 저장)
    override fun registerMedia(userId: Long, request: UserMediaRegisterRequest): UserMediaResponse {
        val user = getUserById(userId)
        val s3Key = normalizeToS3Key(request.s3Key)

        val existing = userMediaRepository.findByS3Key(s3Key)

        val media = if (existing != null) {
            if (existing.user.id != userId) {
                throw BusinessException(GlobalErrorCode.FORBIDDEN, "S3 key belongs to another user.")
            }
            existing
        } else {
            val displayName = resolveDisplayName(
                request.displayName, request.mediaType, s3Key, null, LocalDateTime.now()
            )
            val newMedia = if (request.mediaType == UserMediaType.PHOTO) {
                UserMedia.ofPhoto(user, s3Key, displayName)
            } else {
                UserMedia.ofVideo(user, s3Key, null, null, displayName, null)
            }
            userMediaRepository.save(newMedia)
        }
        return toResponse(media)
    }

    // 내 미디어 목록 조회 (보관기간 cutoff + 타입 필터, 페이지 단위)
    @Transactional(readOnly = true)
    override fun getMyMedia(
        userId: Long,
        mediaType: UserMediaType?,
        page: Int,
        size: Int
    ): PageResponse<UserMediaResponse> {
        val user = getUserById(userId)
        val pageable = createPageable(page, size)
        val cutoff = subscriptionPolicy.resolveHistoryCutoff(user)
        val mediaPage = findMediaPage(user, mediaType, cutoff, pageable)
        return PageResponse.from(mediaPage.map { toResponse(it) })
    }

    // 미디어 다운로드용 presigned URL 발급 (소유권·보관기간 검증)
    override fun getDownloadUrl(userId: Long, mediaId: Long): String {
        val user = getUserById(userId)
        val media = userMediaRepository.findByIdAndUser(mediaId, user)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND)
        subscriptionPolicy.assertHistoryAccessible(user, media.createdAt)

        val downloadName = resolveDisplayNameForView(media)
        return fileStorageService.generatePresignedDownloadUrl(media.s3Key, downloadName)
    }

    // 표시 파일명 수정 (소유권·보관기간 검증 후 정규화해 반영)
    override fun updateDisplayName(
        userId: Long,
        mediaId: Long,
        request: UserMediaDisplayNameUpdateRequest
    ): UserMediaResponse {
        val user = getUserById(userId)
        val media = userMediaRepository.findByIdAndUser(mediaId, user)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND)
        subscriptionPolicy.assertHistoryAccessible(user, media.createdAt)

        val normalized = resolveDisplayName(
            request.displayName, media.mediaType, media.s3Key, media.originalFileName, media.createdAt
        )
        media.changeDisplayName(normalized)
        return toResponse(media)
    }

    // 변환 완료 영상 영속화 (S3 key 기준 멱등, 썸네일 key 포함)
    override fun saveTranscodedVideo(
        userPublicId: String,
        originalFileName: String?,
        outputS3Path: String,
        thumbnailOutputS3Path: String?,
        transcodeJobId: String
    ): UserMediaResponse {
        if (userPublicId.isBlank()) {
            throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "userPublicId must not be blank.")
        }
        val user = getUserByPublicId(userPublicId)
        val outputS3Key = normalizeToS3Key(outputS3Path)

        userMediaRepository.findByS3Key(outputS3Key)?.let {
            log.info("이미 저장된 변환 미디어입니다. 기존 항목을 반환합니다. key={}", outputS3Key)
            return toResponse(it)
        }

        val originalS3Key = if (!originalFileName.isNullOrBlank()) {
            "uploads/users/$userPublicId/webm/$originalFileName"
        } else {
            null
        }
        val preferredName = buildTranscodedDisplayNameFromOriginal(originalFileName)
        val displayName = resolveDisplayName(
            preferredName, UserMediaType.VIDEO, outputS3Key, originalFileName, LocalDateTime.now()
        )
        val thumbnailKey = thumbnailOutputS3Path?.takeIf { it.isNotBlank() }?.let { normalizeToS3Key(it) }

        val media = UserMedia.ofVideo(
            user, outputS3Key, originalS3Key, originalFileName, displayName, transcodeJobId, thumbnailKey
        )
        val saved = userMediaRepository.save(media)
        log.info("변환 미디어 저장 완료. userPublicId={}, outputS3Key={}", userPublicId, outputS3Key)
        return toResponse(saved)
    }

    // ── helpers ─────────────────────

    // userId로 사용자 조회 (없으면 예외)
    private fun getUserById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.") }

    // 엔티티 → 응답 변환 (presigned 다운로드/썸네일 URL 부착)
    private fun toResponse(media: UserMedia): UserMediaResponse {
        val displayName = resolveDisplayNameForView(media)
        val downloadUrl = if (media.mediaType == UserMediaType.VIDEO) {
            null
        } else {
            fileStorageService.generatePresignedDownloadUrl(media.s3Key, displayName)
        }
        val thumbnailUrl = media.thumbnailKey
            ?.takeIf { it.isNotBlank() }
            ?.let { fileStorageService.generatePresignedGetUrl(it) }

        return UserMediaResponse(
            mediaId = media.id,
            mediaType = media.mediaType,
            s3Key = media.s3Key,
            displayName = displayName,
            downloadUrl = downloadUrl,
            thumbnailUrl = thumbnailUrl,
            originalS3Key = media.originalS3Key,
            originalFileName = media.originalFileName,
            transcodeJobId = media.transcodeJobId,
            createdAt = media.createdAt
        )
    }

    // 화면/다운로드에 쓸 표시 파일명 확정 (비어 있으면 fallback 생성)
    private fun resolveDisplayNameForView(media: UserMedia): String {
        if (media.displayName.isNotBlank()) return media.displayName.trim()
        return resolveDisplayName(null, media.mediaType, media.s3Key, media.originalFileName, media.createdAt)
    }

    // 입력(URL/s3:// 경로/key)을 순수 S3 key로 정규화
    private fun normalizeToS3Key(pathOrKey: String?): String {
        if (pathOrKey.isNullOrBlank()) {
            throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "S3 path or key must not be blank.")
        }
        val value = pathOrKey.trim()
        if (value.startsWith("s3://") || value.startsWith("http://") || value.startsWith("https://")) {
            val key = URI.create(value).path
            if (key.isNullOrBlank() || key == "/") {
                throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "Cannot extract S3 key from the given path.")
            }
            return if (key.startsWith("/")) key.substring(1) else key
        }
        return if (value.startsWith("/")) value.substring(1) else value
    }

    // 표시 파일명 생성 (사용자 지정 → 원본 파일명 → harucut_타임스탬프 우선순위, 확장자는 key 기준)
    private fun resolveDisplayName(
        preferredName: String?,
        mediaType: UserMediaType,
        s3Key: String,
        originalFileName: String?,
        baseTime: LocalDateTime?
    ): String {
        val keyExt = extractExtensionWithDot(s3Key)
        var requestedBase = sanitizeBaseName(preferredName)
        if (requestedBase.isBlank() && mediaType == UserMediaType.VIDEO) {
            requestedBase = sanitizeBaseName(removeExtension(originalFileName))
        }
        val finalBase = if (requestedBase.isNotBlank()) {
            requestedBase
        } else {
            FALLBACK_PREFIX + DISPLAY_NAME_TIME_FORMAT.format(baseTime ?: LocalDateTime.now())
        }
        return truncateFileName(finalBase + keyExt, 255)
    }

    // 페이지 파라미터 검증 후 Pageable 생성
    private fun createPageable(page: Int, size: Int): Pageable {
        if (page < 0) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "page must be 0 or greater.")
        if (size < 1) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "size must be 1 or greater.")
        return PageRequest.of(page, size)
    }

    // cutoff·타입 조합에 맞는 페이지 쿼리 선택
    private fun findMediaPage(
        user: User,
        mediaType: UserMediaType?,
        cutoff: LocalDateTime?,
        pageable: Pageable
    ): Page<UserMedia> {
        if (cutoff == null) {
            return if (mediaType == null) {
                userMediaRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
            } else {
                userMediaRepository.findAllByUserAndMediaTypeOrderByCreatedAtDesc(user, mediaType, pageable)
            }
        }
        return if (mediaType == null) {
            userMediaRepository.findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, cutoff, pageable)
        } else {
            userMediaRepository.findAllByUserAndMediaTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                user, mediaType, cutoff, pageable
            )
        }
    }

    // 파일명에서 경로·위험문자·확장자 제거해 base만 추출
    private fun sanitizeBaseName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        var value = name.trim()
            .replace("\\", "/")
            .replace("\"", "")
            .replace("\r", "")
            .replace("\n", "")
        val slashIdx = value.lastIndexOf('/')
        if (slashIdx in 0 until value.length - 1) {
            value = value.substring(slashIdx + 1)
        }
        return removeExtension(value)
            .replace(Regex("\\p{Cntrl}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // 점 포함 확장자 안전 추출 (화이트리스트 형태만 인정)
    private fun extractExtensionWithDot(filenameOrKey: String?): String {
        if (filenameOrKey.isNullOrBlank()) return ""
        val idx = filenameOrKey.lastIndexOf('.')
        if (idx < 0 || idx == filenameOrKey.length - 1) return ""
        val ext = filenameOrKey.substring(idx).lowercase()
        return if (ext.matches(Regex("\\.[a-z0-9]{1,10}"))) ext else ""
    }

    // 확장자 제거 (맨 앞 점은 보존)
    private fun removeExtension(filename: String?): String {
        if (filename.isNullOrBlank()) return ""
        val idx = filename.lastIndexOf('.')
        return if (idx <= 0) filename else filename.substring(0, idx)
    }

    // 확장자를 보존하며 최대 길이로 절단
    private fun truncateFileName(fileName: String, maxLength: Int): String {
        if (fileName.isBlank() || fileName.length <= maxLength) return fileName
        val ext = extractExtensionWithDot(fileName)
        val baseLimit = maxLength - ext.length
        if (baseLimit <= 0) return fileName.substring(0, maxLength)
        var base = removeExtension(fileName)
        if (base.length > baseLimit) base = base.substring(0, baseLimit)
        return base + ext
    }

    // publicId로 사용자 조회 (없으면 예외)
    private fun getUserByPublicId(publicId: String): User =
        userRepository.findByPublicId(publicId)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.")

    // 원본 파일명으로 변환본 표시 파일명(.mp4) 생성
    private fun buildTranscodedDisplayNameFromOriginal(originalFileName: String?): String? {
        val base = sanitizeBaseName(removeExtension(originalFileName))
        return if (base.isNotBlank()) "$base.mp4" else null
    }

    companion object {
        private const val FALLBACK_PREFIX = "harucut_"
        private val DISPLAY_NAME_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
