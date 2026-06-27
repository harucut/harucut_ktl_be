package com.harucut.frame.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.attributes.BackgroundAttributes
import com.harucut.frame.attributes.ImageBackgroundAttributes
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

    override fun createFrame(userId: Long, request: FrameCreateRequest) {
        val user = getUserById(userId)
        frameSubscriptionPolicy.assertFrameRetentionLimit(user, frameRepository.countByUser(user).toInt())

        val resolvedKeyMap = frameAssetManager.normalizeComponentKeys(request.components)
        val resolvedBackground = normalizeBackground(request.background)
        val resolvedPreviewKey = frameAssetManager.normalizeKey(request.previewKey) ?: request.previewKey

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

    @Transactional(readOnly = true)
    override fun getMyFrames(userId: Long): List<FrameResponse> {
        val user = getUserById(userId)
        val cutoff = frameSubscriptionPolicy.resolveHistoryCutoff(user)

        return frameRepository.findAllByUserOrderByCreatedAtDesc(user)
            .filter { isWithinHistoryWindow(it.createdAt, cutoff) }
            .map { toFrameResponse(it) }
    }

    @Transactional(readOnly = true)
    override fun getFrame(frameId: Long, userId: Long): FrameResponse {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)
        frameSubscriptionPolicy.assertHistoryAccessible(frame.user, frame.createdAt)
        return toFrameResponse(frame)
    }

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

    override fun updateFrame(userId: Long, frameId: Long, request: FrameCreateRequest) {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)

        val oldBackgroundKey = extractBackgroundKey(frame.background)
        val oldPreviewKey = frame.previewKey
        val oldPhotoKeys = extractPhotoKeys(frame.components)

        val newBackground = normalizeBackground(request.background)
        val newBackgroundKey = extractBackgroundKey(newBackground)
        val newPreviewKey = frameAssetManager.normalizeKey(request.previewKey) ?: request.previewKey

        frame.updateMetadata(request.title, request.description ?: "", newBackground, newPreviewKey)

        if (oldBackgroundKey != null && oldBackgroundKey != newBackgroundKey) {
            frameAssetManager.deleteFiles(listOf(oldBackgroundKey))
        }
        if (oldPreviewKey != newPreviewKey) {
            frameAssetManager.deleteFiles(listOf(oldPreviewKey))
        }

        frame.clearComponents()
        val resolvedKeyMap = frameAssetManager.normalizeComponentKeys(request.components)
        val newComponents = createComponents(request.components, resolvedKeyMap)
        newComponents.forEach(frame::addComponent)

        val newPhotoKeys = extractPhotoKeys(newComponents).toSet()
        val garbageKeys = oldPhotoKeys.filterNot { it in newPhotoKeys }
        frameAssetManager.deleteFiles(garbageKeys)
    }

    // ── helpers ────────────────────────────

    private fun getUserById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.") }

    private fun findFrameById(frameId: Long): Frame =
        frameRepository.findById(frameId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND) }

    private fun validateOwner(frame: Frame, userId: Long) {
        if (frame.user.id != userId) throw BusinessException(GlobalErrorCode.FORBIDDEN, "권한이 없습니다.")
    }

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

    private fun extractPhotoKeys(components: List<FrameComponent>): List<String> =
        components.filter { it.type == ComponentType.PHOTO }.map { it.source }

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

    private fun resolveBackgroundUrl(bg: BackgroundAttributes): BackgroundAttributes =
        when (bg) {
            is ImageBackgroundAttributes ->
                ImageBackgroundAttributes(
                    frameAssetManager.resolveSource(BackgroundType.IMAGE, bg.key) ?: bg.key,
                    bg.opacity
                )

            else -> bg
        }

    private fun normalizeBackground(bg: BackgroundAttributes): BackgroundAttributes =
        when (bg) {
            is ImageBackgroundAttributes -> {
                val normalized = frameAssetManager.normalizeKey(bg.key) ?: bg.key
                if (normalized != bg.key) ImageBackgroundAttributes(normalized, bg.opacity) else bg
            }

            else -> bg
        }

    private fun extractBackgroundKey(bg: BackgroundAttributes): String? =
        when (bg) {
            is ImageBackgroundAttributes -> bg.key
            else -> null
        }

    private fun isWithinHistoryWindow(createdAt: LocalDateTime?, cutoff: LocalDateTime?): Boolean {
        if (cutoff == null || createdAt == null) return true
        return !createdAt.isBefore(cutoff)
    }
}
