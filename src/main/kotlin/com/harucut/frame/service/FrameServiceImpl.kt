package com.harucut.frame.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.component.FrameAssetManager
import com.harucut.frame.component.FrameComponentAssembler
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.entity.Frame
import com.harucut.frame.policy.FrameSubscriptionPolicy
import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.exception.SubscriptionErrorCode
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
    private val frameSubscriptionPolicy: FrameSubscriptionPolicy,
    private val frameComponentAssembler: FrameComponentAssembler
) : FrameService {

    override fun createFrame(userId: Long, request: FrameCreateRequest) {
        val user = getUserById(userId)
        frameSubscriptionPolicy.assertFrameRetentionLimit(user, frameRepository.countByUser(user).toInt())

        val resolvedKeyMap = frameAssetManager.normalizeComponentKeys(request.components)
        val resolvedBackground = frameComponentAssembler.normalizeBackground(request.background)
        val resolvedPreviewKey = frameAssetManager.normalizeKey(request.previewKey) ?: request.previewKey

        val frame = Frame(
            title = request.title,
            description = request.description ?: "",
            previewKey = resolvedPreviewKey,
            frameType = request.frameType,
            background = resolvedBackground,
            user = user
        )
        frameComponentAssembler.createComponents(request.components, resolvedKeyMap).forEach(frame::addComponent)

        frameRepository.save(frame)
    }

    @Transactional(readOnly = true)
    override fun getMyFrames(userId: Long): List<FrameResponse> {
        val user = getUserById(userId)
        val cutoff = frameSubscriptionPolicy.resolveHistoryCutoff(user)
        val cap = frameSubscriptionPolicy.resolveFrameRetentionCap(user)

        // 시스템 프레임은 항상 노출되며 retention cutoff/소프트 캡 대상이 아니다.
        val systemFrames = frameRepository.findAllByIsSystemTrueOrderByCreatedAtDesc()
        val userFrames = frameRepository.findAllByUserOrderByCreatedAtDesc(user)
            .filter { isWithinHistoryWindow(it.createdAt, cutoff) }
            .let { if (cap != null) it.take(cap) else it }

        return (userFrames + systemFrames).map { frameComponentAssembler.toFrameResponse(it) }
    }

    @Transactional(readOnly = true)
    override fun getFrame(frameId: Long, userId: Long): FrameResponse {
        val frame = findFrameById(frameId)
        // 시스템 프레임은 소유자 검사·보관 기간/소프트 캡을 우회하고 누구나 읽을 수 있다.
        if (!frame.isSystem) {
            validateOwner(frame, userId)
            frameSubscriptionPolicy.assertHistoryAccessible(frame.user!!, frame.createdAt)
            assertWithinRetentionCap(frame)
        }
        return frameComponentAssembler.toFrameResponse(frame)
    }

    override fun deleteFrame(userId: Long, frameId: Long) {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)

        val keysToDelete = buildList {
            addAll(frameComponentAssembler.extractPhotoKeys(frame.components))
            frameComponentAssembler.extractBackgroundKey(frame.background)?.let { add(it) }
            add(frame.previewKey)
        }
        frameAssetManager.deleteFiles(keysToDelete)

        frameRepository.delete(frame)
    }

    override fun updateFrame(userId: Long, frameId: Long, request: FrameCreateRequest) {
        val frame = findFrameById(frameId)
        validateOwner(frame, userId)

        val oldBackgroundKey = frameComponentAssembler.extractBackgroundKey(frame.background)
        val oldPreviewKey = frame.previewKey
        val oldPhotoKeys = frameComponentAssembler.extractPhotoKeys(frame.components)

        val newBackground = frameComponentAssembler.normalizeBackground(request.background)
        val newBackgroundKey = frameComponentAssembler.extractBackgroundKey(newBackground)
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
        val newComponents = frameComponentAssembler.createComponents(request.components, resolvedKeyMap)
        newComponents.forEach(frame::addComponent)

        val newPhotoKeys = frameComponentAssembler.extractPhotoKeys(newComponents).toSet()
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
        if (frame.user?.id != userId) throw BusinessException(GlobalErrorCode.FORBIDDEN, "권한이 없습니다.")
    }

    // 소프트 캡 판정: 자기보다 최신인 프레임 수가 cap 이상이면 접근 차단 (시스템 프레임은 호출되지 않음)
    private fun assertWithinRetentionCap(frame: Frame) {
        val owner = frame.user ?: return
        val cap = frameSubscriptionPolicy.resolveFrameRetentionCap(owner) ?: return
        val createdAt = frame.createdAt ?: return
        if (frameRepository.countByUserAndCreatedAtAfter(owner, createdAt) >= cap) {
            throw BusinessException(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }
    }

    private fun isWithinHistoryWindow(createdAt: LocalDateTime?, cutoff: LocalDateTime?): Boolean {
        if (cutoff == null || createdAt == null) return true
        return !createdAt.isBefore(cutoff)
    }
}
