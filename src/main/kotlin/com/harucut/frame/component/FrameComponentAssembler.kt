package com.harucut.frame.component

import com.harucut.frame.attributes.BackgroundAttributes
import com.harucut.frame.attributes.ImageBackgroundAttributes
import com.harucut.frame.converter.FrameStyleConverter
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.entity.Frame
import com.harucut.frame.entity.FrameComponent
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import org.springframework.stereotype.Component

// 사용자 프레임(FrameServiceImpl)과 관리자 시스템 프레임(FrameAdminServiceImpl)이 공통으로 쓰는
// 배경/컴포넌트 정규화·조립·응답 변환 로직. 두 서비스가 서로의 구현체가 아닌 이 컴포넌트에 의존한다.
@Component
class FrameComponentAssembler(
    private val frameAssetManager: FrameAssetManager,
    private val frameStyleConverter: FrameStyleConverter
) {

    fun createComponents(
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

    fun extractPhotoKeys(components: List<FrameComponent>): List<String> =
        components.filter { it.type == ComponentType.PHOTO }.map { it.source }

    fun toFrameResponse(frame: Frame): FrameResponse {
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
            components = componentResponses,
            isSystem = frame.isSystem
        )
    }

    fun normalizeBackground(bg: BackgroundAttributes): BackgroundAttributes =
        when (bg) {
            is ImageBackgroundAttributes -> {
                val normalized = frameAssetManager.normalizeKey(bg.key) ?: bg.key
                if (normalized != bg.key) ImageBackgroundAttributes(normalized, bg.opacity) else bg
            }

            else -> bg
        }

    fun extractBackgroundKey(bg: BackgroundAttributes): String? =
        when (bg) {
            is ImageBackgroundAttributes -> bg.key
            else -> null
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
}
