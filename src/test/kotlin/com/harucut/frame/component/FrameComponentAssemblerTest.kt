package com.harucut.frame.component

import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.attributes.ImageBackgroundAttributes
import com.harucut.frame.converter.FrameStyleConverter
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.entity.Frame
import com.harucut.frame.entity.FrameComponent
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.frame.enums.FrameType
import com.harucut.user.entity.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FrameComponentAssemblerTest {

    private val frameAssetManager = mockk<FrameAssetManager>()
    private val frameStyleConverter = mockk<FrameStyleConverter>(relaxed = true)
    private val assembler = FrameComponentAssembler(frameAssetManager, frameStyleConverter)

    private fun componentRequest(source: String, type: ComponentType = ComponentType.PHOTO) =
        FrameCreateRequest.ComponentRequest(type = type, source = source)

    private fun userFrame(): Frame = Frame(
        title = "내 프레임",
        description = "설명",
        previewKey = "uploads/users/pub/preview.png",
        frameType = FrameType.CLASSIC,
        background = ColorBackgroundAttributes("#ffffff"),
        user = mockk<User>(relaxed = true)
    )

    private fun systemFrame(): Frame = Frame.system(
        title = "기본 프레임",
        description = "설명",
        previewKey = "uploads/system/preview.png",
        frameType = FrameType.CLASSIC,
        background = ColorBackgroundAttributes("#ffffff")
    )

    @Nested
    inner class CreateComponents {

        @Test
        @DisplayName("요청 목록을 정규화된 key로 컴포넌트 목록으로 변환한다")
        fun success() {
            every { frameStyleConverter.convertToJson(any()) } returns "{}"
            val resolvedKeyMap = mapOf("raw-source" to "uploads/users/pub/components/a.png")

            val result = assembler.createComponents(listOf(componentRequest("raw-source")), resolvedKeyMap)

            assertThat(result).hasSize(1)
            assertThat(result[0].source).isEqualTo("uploads/users/pub/components/a.png")
        }

        @Test
        @DisplayName("요청이 null이면 빈 목록을 반환한다")
        fun nullRequests() {
            val result = assembler.createComponents(null, emptyMap())

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class ExtractPhotoKeys {

        @Test
        @DisplayName("PHOTO 타입 컴포넌트의 source만 추출한다")
        fun filtersPhotoOnly() {
            val components = listOf(
                FrameComponent(
                    source = "uploads/users/pub/components/photo.png",
                    type = ComponentType.PHOTO,
                    x = 0.0, y = 0.0, width = null, height = null, scale = null,
                    rotation = 0.0, zIndex = 0, styleJson = null
                ),
                FrameComponent(
                    source = "uploads/users/pub/components/sticker.png",
                    type = ComponentType.STICKER,
                    x = 0.0, y = 0.0, width = null, height = null, scale = null,
                    rotation = 0.0, zIndex = 0, styleJson = null
                )
            )

            val result = assembler.extractPhotoKeys(components)

            assertThat(result).containsExactly("uploads/users/pub/components/photo.png")
        }
    }

    @Nested
    inner class ToFrameResponse {

        @Test
        @DisplayName("사용자 프레임을 응답 DTO로 변환하면 isSystem=false 이다")
        fun mapsUserFrame() {
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = assembler.toFrameResponse(userFrame())

            assertThat(result.title).isEqualTo("내 프레임")
            assertThat(result.source).isEqualTo("preview-url")
            assertThat(result.isSystem).isFalse()
        }

        @Test
        @DisplayName("[회귀] 시스템 프레임을 응답 DTO로 변환하면 isSystem=true 이다")
        fun mapsSystemFrame() {
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = assembler.toFrameResponse(systemFrame())

            assertThat(result.title).isEqualTo("기본 프레임")
            assertThat(result.isSystem).isTrue()
        }
    }

    @Nested
    inner class NormalizeBackground {

        @Test
        @DisplayName("이미지 배경의 key를 정규화한다")
        fun normalizesImageKey() {
            every { frameAssetManager.normalizeKey("raw-key") } returns "uploads/users/pub/bg.png"

            val result = assembler.normalizeBackground(ImageBackgroundAttributes("raw-key", 1.0))

            assertThat(result).isInstanceOf(ImageBackgroundAttributes::class.java)
            assertThat((result as ImageBackgroundAttributes).key).isEqualTo("uploads/users/pub/bg.png")
        }

        @Test
        @DisplayName("이미지가 아닌 배경은 정규화하지 않고 그대로 반환한다")
        fun passesThroughNonImage() {
            val bg = ColorBackgroundAttributes("#ffffff")

            val result = assembler.normalizeBackground(bg)

            assertThat(result).isSameAs(bg)
        }
    }

    @Nested
    inner class ExtractBackgroundKey {

        @Test
        @DisplayName("이미지 배경이면 key를 반환한다")
        fun imageBackground() {
            val result = assembler.extractBackgroundKey(ImageBackgroundAttributes("uploads/users/pub/bg.png", 1.0))

            assertThat(result).isEqualTo("uploads/users/pub/bg.png")
        }

        @Test
        @DisplayName("이미지가 아니면 null을 반환한다")
        fun nonImageBackground() {
            val result = assembler.extractBackgroundKey(ColorBackgroundAttributes("#ffffff"))

            assertThat(result).isNull()
        }
    }
}
