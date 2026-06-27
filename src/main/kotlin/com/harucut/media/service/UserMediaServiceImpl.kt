package com.harucut.media.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.media.dto.UserMediaDisplayNameUpdateRequest
import com.harucut.media.dto.UserMediaRegisterRequest
import com.harucut.media.dto.UserMediaResponse
import com.harucut.media.entity.UserMedia
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.media.repository.UserMediaRepository
import com.harucut.storage.service.FileStorageService
import com.harucut.storage.util.normalizeToS3Key
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import com.harucut.util.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
            val displayName = resolveDisplayName(request.displayName, s3Key, LocalDateTime.now())
            val newMedia = UserMedia.ofPhoto(user, s3Key, displayName)
            userMediaRepository.save(newMedia)
        }
        return toResponse(media)
    }

    @Transactional(readOnly = true)
    override fun getMyMedia(userId: Long, page: Int, size: Int): PageResponse<UserMediaResponse> {
        val user = getUserById(userId)
        val pageable = createPageable(page, size)
        val cutoff = subscriptionPolicy.resolveHistoryCutoff(user)

        val mediaPage = if (cutoff == null) {
            userMediaRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
        } else {
            userMediaRepository.findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, cutoff, pageable)
        }
        return PageResponse.from(mediaPage.map { toResponse(it) })
    }

    override fun getDownloadUrl(userId: Long, mediaId: Long): String {
        val user = getUserById(userId)
        val media = userMediaRepository.findByIdAndUser(mediaId, user)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND)
        subscriptionPolicy.assertHistoryAccessible(user, media.createdAt)

        val downloadName = media.displayName.ifBlank {
            resolveDisplayName(null, media.s3Key, media.createdAt)
        }
        return fileStorageService.generatePresignedDownloadUrl(media.s3Key, downloadName)
    }

    override fun updateDisplayName(
        userId: Long,
        mediaId: Long,
        request: UserMediaDisplayNameUpdateRequest
    ): UserMediaResponse {
        val user = getUserById(userId)
        val media = userMediaRepository.findByIdAndUser(mediaId, user)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND)
        subscriptionPolicy.assertHistoryAccessible(user, media.createdAt)

        val normalized = resolveDisplayName(request.displayName, media.s3Key, media.createdAt)
        media.changeDisplayName(normalized)
        return toResponse(media)
    }

    // ── helpers ─────────────────────

    private fun getUserById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.") }

    private fun toResponse(media: UserMedia): UserMediaResponse {
        val displayName = media.displayName.ifBlank {
            resolveDisplayName(null, media.s3Key, media.createdAt)
        }
        val downloadUrl = fileStorageService.generatePresignedDownloadUrl(media.s3Key, displayName)
        return UserMediaResponse(
            mediaId = media.id,
            s3Key = media.s3Key,
            displayName = displayName,
            downloadUrl = downloadUrl,
            createdAt = media.createdAt
        )
    }

    private fun resolveDisplayName(
        preferredName: String?,
        s3Key: String,
        baseTime: LocalDateTime?
    ): String {
        val keyExt = extractExtensionWithDot(s3Key)
        val requestedBase = sanitizeBaseName(preferredName)
        val finalBase = if (requestedBase.isNotBlank()) {
            requestedBase
        } else {
            FALLBACK_PREFIX + DISPLAY_NAME_TIME_FORMAT.format(baseTime ?: LocalDateTime.now())
        }
        return truncateFileName(finalBase + keyExt, 255)
    }

    private fun createPageable(page: Int, size: Int): Pageable {
        if (page < 0) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "page must be 0 or greater.")
        if (size < 1) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "size must be 1 or greater.")
        return PageRequest.of(page, size)
    }

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

    private fun extractExtensionWithDot(filenameOrKey: String?): String {
        if (filenameOrKey.isNullOrBlank()) return ""
        val idx = filenameOrKey.lastIndexOf('.')
        if (idx < 0 || idx == filenameOrKey.length - 1) return ""
        val ext = filenameOrKey.substring(idx).lowercase()
        return if (ext.matches(Regex("\\.[a-z0-9]{1,10}"))) ext else ""
    }

    private fun removeExtension(filename: String?): String {
        if (filename.isNullOrBlank()) return ""
        val idx = filename.lastIndexOf('.')
        return if (idx <= 0) filename else filename.substring(0, idx)
    }

    private fun truncateFileName(fileName: String, maxLength: Int): String {
        if (fileName.isBlank() || fileName.length <= maxLength) return fileName
        val ext = extractExtensionWithDot(fileName)
        val baseLimit = maxLength - ext.length
        if (baseLimit <= 0) return fileName.substring(0, maxLength)
        var base = removeExtension(fileName)
        if (base.length > baseLimit) base = base.substring(0, baseLimit)
        return base + ext
    }

    companion object {
        private const val FALLBACK_PREFIX = "harucut_"
        private val DISPLAY_NAME_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
