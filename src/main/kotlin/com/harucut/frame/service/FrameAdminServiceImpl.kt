package com.harucut.frame.service

import com.harucut.exception.BusinessException
import com.harucut.frame.component.FrameAssetManager
import com.harucut.frame.component.FrameComponentAssembler
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.entity.Frame
import com.harucut.frame.exception.FrameErrorCode
import com.harucut.frame.repository.FrameRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 기본 제공(시스템) 프레임 관리자 CRUD. 생성/수정/조회에 필요한 정규화·컴포넌트 조립·응답 변환 로직은
// FrameComponentAssembler(공용 컴포넌트)를 재사용해 사용자 프레임 로직과 중복되지 않도록 한다.
@Service
@Transactional
class FrameAdminServiceImpl(
    private val frameRepository: FrameRepository,
    private val frameAssetManager: FrameAssetManager,
    private val frameComponentAssembler: FrameComponentAssembler
) : FrameAdminService {

    override fun createSystemFrame(request: FrameCreateRequest) {
        val resolvedKeyMap = frameAssetManager.normalizeComponentKeys(request.components)
        val resolvedBackground = frameComponentAssembler.normalizeBackground(request.background)
        val resolvedPreviewKey = frameAssetManager.normalizeKey(request.previewKey) ?: request.previewKey

        val frame = Frame.system(
            title = request.title,
            description = request.description ?: "",
            previewKey = resolvedPreviewKey,
            frameType = request.frameType,
            background = resolvedBackground
        )
        frameComponentAssembler.createComponents(request.components, resolvedKeyMap).forEach(frame::addComponent)

        frameRepository.save(frame)
    }

    override fun updateSystemFrame(frameId: Long, request: FrameCreateRequest) {
        val frame = findSystemFrame(frameId)

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

    override fun deleteSystemFrame(frameId: Long) {
        val frame = findSystemFrame(frameId)

        val keysToDelete = buildList {
            addAll(frameComponentAssembler.extractPhotoKeys(frame.components))
            frameComponentAssembler.extractBackgroundKey(frame.background)?.let { add(it) }
            add(frame.previewKey)
        }
        frameAssetManager.deleteFiles(keysToDelete)

        frameRepository.delete(frame)
    }

    @Transactional(readOnly = true)
    override fun listSystemFrames(): List<FrameResponse> =
        frameRepository.findAllByIsSystemTrueOrderByCreatedAtDesc().map { frameComponentAssembler.toFrameResponse(it) }

    // 대상이 없거나 시스템 프레임이 아니면(=사용자 프레임을 admin API로 조작 시도) 방어적으로 404 처리
    private fun findSystemFrame(frameId: Long): Frame {
        val frame = frameRepository.findById(frameId)
            .orElseThrow { BusinessException(FrameErrorCode.SYSTEM_FRAME_NOT_FOUND) }
        if (!frame.isSystem) throw BusinessException(FrameErrorCode.SYSTEM_FRAME_NOT_FOUND)
        return frame
    }
}
