package com.harucut.frame.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.attributes.BackgroundAttributes
import com.harucut.frame.attributes.ImageBackgroundAttributes
import com.harucut.frame.attributes.VideoBackgroundAttributes
import com.harucut.frame.component.FrameAssetManager
import com.harucut.frame.converter.FrameStyleConverter
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.entity.Frame
import com.harucut.frame.entity.FrameComponent
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.frame.policy.FrameSubscriptionPolicy
import com.harucut.frame.repository.FrameRepository
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class FrameServiceImpl(
    private val frameRepository: FrameRepository,
    private val userRepository: UserRepository,
    private val frameAssetManager: FrameAssetManager,
    private val frameStyleConverter: FrameStyleConverter,
    private val frameSubscriptionPolicy: FrameSubscriptionPolicy
) : FrameService {

    // 프레임 생성 (동시 보관 cap 검증 → temp 자산 승격 → 컴포넌트와 함께 저장)
    override fun createFrame(userId: Long, request: FrameCreateRequest) {
        val user = getUserById(userId)
        frameSubscriptionPolicy.assertFrameRetentionLimit(user, frameRepository.countByUser(user).toInt())

        val resolvedKeyMap = frameAssetManager.moveTempFilesToPermanent(user, request.components)
        val resolvedBackground = promoteBackground(user, request.background)
        val resolvedPreviewKey = frameAssetManager.moveTempFileToPermanent(user, request.previewKey)
            ?: request.previewKey

        val frame = Frame(
            title = request.title,
            description = request.description ?: "",
            previewKey = resolvedPreviewKey,
            frameType = request.frameType,
            background = resolvedBackground,
            user = user
        )
        createComponents(request.components, resolvedKeyMap).forEach(frame::addComponent)

        frameRepository.save(frame)
    }

    // 내 프레임 목록 조회 (보관 기간 cutoff 이내만)
    @Transactional(readOnly = true)
    override fun getMyFrames(userId: Long): List<FrameResponse> {
        val user = getUserById(userId)
        val cutoff = frameSubscriptionPolicy.resolveHistoryCutoff(user)

        return frameRepository.findAllByUserOrderByCreatedAtDesc(user)
            .filter { isWithinHistoryWindow(it.createdAt, cutoff) }
            .map { toFrameResponse(it) }
    }

    // 프레임 단건 조회 (소유권 + 보관 기간 검증)
    @Transactional(readOnly = true)
    override fun getFrame(frameId: Long, userId: Long): FrameResponse {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)
        frameSubscriptionPolicy.assertHistoryAccessible(frame.user, frame.createdAt)
        return toFrameResponse(frame)
    }

    // 프레임 삭제 (연결된 S3 자산 정리 후 행 삭제)
    override fun deleteFrame(userId: Long, frameId: Long) {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)

        val keysToDelete = buildList {
            addAll(extractPhotoKeys(frame.components))
            extractBackgroundKey(frame.background)?.let { add(it) }
            add(frame.previewKey)
        }
        frameAssetManager.deleteFiles(keysToDelete)

        frameRepository.delete(frame)
    }

    // 프레임 수정 (메타데이터 교체 + 컴포넌트 재구성 + 미사용 자산 정리)
    override fun updateFrame(userId: Long, frameId: Long, request: FrameCreateRequest) {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)

        val oldBackgroundKey = extractBackgroundKey(frame.background)
        val oldPreviewKey = frame.previewKey
        val oldPhotoKeys = extractPhotoKeys(frame.components)

        val newBackground = promoteBackground(frame.user, request.background)
        val newBackgroundKey = extractBackgroundKey(newBackground)
        val newPreviewKey = frameAssetManager.moveTempFileToPermanent(frame.user, request.previewKey)
            ?: request.previewKey

        frame.updateMetadata(request.title, request.description ?: "", newBackground, newPreviewKey)

        // 교체된 배경/프리뷰 자산 삭제
        if (oldBackgroundKey != null && oldBackgroundKey != newBackgroundKey) {
            frameAssetManager.deleteFiles(listOf(oldBackgroundKey))
        }
        if (oldPreviewKey != newPreviewKey) {
            frameAssetManager.deleteFiles(listOf(oldPreviewKey))
        }

        // 컴포넌트 전체 교체
        frame.clearComponents()
        val resolvedKeyMap = frameAssetManager.moveTempFilesToPermanent(frame.user, request.components)
        val newComponents = createComponents(request.components, resolvedKeyMap)
        newComponents.forEach(frame::addComponent)

        // 더 이상 쓰이지 않는 사진 자산 정리
        val newPhotoKeys = extractPhotoKeys(newComponents).toSet()
        val garbageKeys = oldPhotoKeys.filterNot { it in newPhotoKeys }
        frameAssetManager.deleteFiles(garbageKeys)
    }

    // ── helpers ────────────────────────────

    // userId로 사용자 조회 (없으면 예외)
    private fun getUserById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.") }

    // frameId로 프레임 조회 (없으면 예외)
    private fun findFrameById(frameId: Long): Frame =
        frameRepository.findById(frameId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND) }

    // 프레임 소유자 검증
    private fun validateOwner(frame: Frame, userId: Long) {
        if (frame.user.id != userId) throw BusinessException(GlobalErrorCode.FORBIDDEN, "권한이 없습니다.")
    }

    // 요청 컴포넌트 DTO들을 엔티티로 변환 (소스 key는 승격된 최종 key로 치환)
    private fun createComponents(
        requests: List<FrameCreateRequest.ComponentRequest>?,
        resolvedKeyMap: Map<String, String>
    ): List<FrameComponent> {
        if (requests == null) return emptyList()
        return requests.map { dto ->
            FrameComponent(
                source = resolvedKeyMap.getOrDefault(dto.source, dto.source),
                type = dto.type,
                x = dto.x, y = dto.y,
                width = dto.width, height = dto.height, scale = dto.scale,
                rotation = dto.rotation, zIndex = dto.zIndex,
                styleJson = frameStyleConverter.convertToJson(dto.styleJson)
            )
        }
    }

    // 컴포넌트 중 PHOTO 타입의 S3 key만 추출
    private fun extractPhotoKeys(components: List<FrameComponent>): List<String> =
        components.filter { it.type == ComponentType.PHOTO }.map { it.source }

    // 엔티티 → 응답 DTO 변환 (presigned URL 부착)
    private fun toFrameResponse(frame: Frame): FrameResponse {
        val componentResponses = frame.components.map { c ->
            FrameResponse.ComponentResponse(
                id = c.id,
                type = c.type,
                source = frameAssetManager.resolveSource(c.type, c.source),
                key = c.source,
                x = c.x, y = c.y,
                width = c.width ?: 0.0, height = c.height ?: 0.0,
                rotation = c.rotation, zIndex = c.zIndex,
                style = frameStyleConverter.convertToMap(c.styleJson)
            )
        }
        return FrameResponse(
            frameId = frame.id,
            title = frame.title,
            description = frame.description,
            source = frameAssetManager.resolveSource(BackgroundType.IMAGE, frame.previewKey),
            frameType = frame.frameType,
            background = resolveBackgroundUrl(frame.background),
            components = componentResponses
        )
    }

    // 배경 속성의 S3 key를 presigned URL로 바꾼 응답용 배경 생성
    private fun resolveBackgroundUrl(bg: BackgroundAttributes): BackgroundAttributes =
        when (bg) {
            is ImageBackgroundAttributes ->
                ImageBackgroundAttributes(
                    frameAssetManager.resolveSource(BackgroundType.IMAGE, bg.key) ?: bg.key,
                    bg.opacity
                )

            is VideoBackgroundAttributes ->
                VideoBackgroundAttributes(
                    frameAssetManager.resolveSource(BackgroundType.VIDEO, bg.key) ?: bg.key,
                    bg.autoPlay,
                    bg.loop
                )

            else -> bg
        }

    // 배경 속성의 temp key를 영구 경로로 승격한 새 배경 반환
    private fun promoteBackground(user: User, bg: BackgroundAttributes): BackgroundAttributes =
        when (bg) {
            is ImageBackgroundAttributes -> {
                val moved = frameAssetManager.moveTempFileToPermanent(user, bg.key) ?: bg.key
                if (moved != bg.key) ImageBackgroundAttributes(moved, bg.opacity) else bg
            }

            is VideoBackgroundAttributes -> {
                val moved = frameAssetManager.moveTempFileToPermanent(user, bg.key) ?: bg.key
                if (moved != bg.key) VideoBackgroundAttributes(moved, bg.autoPlay, bg.loop) else bg
            }

            else -> bg
        }

    // 배경 속성에서 S3 key 추출 (색상 배경은 null)
    private fun extractBackgroundKey(bg: BackgroundAttributes): String? =
        when (bg) {
            is ImageBackgroundAttributes -> bg.key
            is VideoBackgroundAttributes -> bg.key
            else -> null
        }

    // createdAt이 보관 기간 cutoff 이내인지
    private fun isWithinHistoryWindow(createdAt: LocalDateTime?, cutoff: LocalDateTime?): Boolean {
        if (cutoff == null || createdAt == null) return true
        return !createdAt.isBefore(cutoff)
    }
}