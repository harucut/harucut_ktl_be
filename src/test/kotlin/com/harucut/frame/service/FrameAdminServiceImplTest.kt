package com.harucut.frame.service

import com.harucut.exception.BusinessException
import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.component.FrameAssetManager
import com.harucut.frame.component.FrameComponentAssembler
import com.harucut.frame.converter.FrameStyleConverter
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.entity.Frame
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.FrameType
import com.harucut.frame.exception.FrameErrorCode
import com.harucut.frame.repository.FrameRepository
import com.harucut.user.entity.User
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

class FrameAdminServiceImplTest {

    private val frameRepository = mockk<FrameRepository>()
    private val frameAssetManager = mockk<FrameAssetManager>()
    private val frameStyleConverter = mockk<FrameStyleConverter>(relaxed = true)

    // FrameAdminServiceImpl은 FrameComponentAssembler(공용 컴포넌트)의 컴포넌트 조립/배경 정규화/응답 변환 로직을 재사용한다.
    private val frameComponentAssembler = FrameComponentAssembler(frameAssetManager, frameStyleConverter)

    private val service = FrameAdminServiceImpl(frameRepository, frameAssetManager, frameComponentAssembler)

    private fun request(title: String = "기본 프레임", previewKey: String = "uploads/system/preview.png") =
        FrameCreateRequest(
            title = title,
            description = "설명",
            previewKey = previewKey,
            frameType = FrameType.CLASSIC,
            background = ColorBackgroundAttributes("#ffffff"),
            components = null
        )

    private fun systemFrame(title: String = "t", previewKey: String = "preview.png"): Frame =
        Frame.system(
            title = title,
            description = "d",
            previewKey = previewKey,
            frameType = FrameType.CLASSIC,
            background = ColorBackgroundAttributes("#fff")
        )

    private fun userFrame(): Frame = Frame(
        title = "t",
        description = "d",
        previewKey = "p",
        frameType = FrameType.CLASSIC,
        background = ColorBackgroundAttributes("#fff"),
        user = mockk<User>(relaxed = true)
    )

    @Nested
    inner class CreateSystemFrame {

        @Test
        @DisplayName("user=null, isSystem=true 인 시스템 프레임을 저장한다")
        fun success() {
            every { frameAssetManager.normalizeComponentKeys(any()) } returns emptyMap()
            every { frameAssetManager.normalizeKey("uploads/system/preview.png") } returns "uploads/system/preview.png"
            val saved = slot<Frame>()
            every { frameRepository.save(capture(saved)) } answers { saved.captured }

            service.createSystemFrame(request())

            assertThat(saved.captured.user).isNull()
            assertThat(saved.captured.isSystem).isTrue()
            assertThat(saved.captured.title).isEqualTo("기본 프레임")
        }
    }

    @Nested
    inner class ListSystemFrames {

        @Test
        @DisplayName("시스템 프레임 목록을 응답 DTO로 변환해 반환한다")
        fun success() {
            every { frameRepository.findAllByIsSystemTrueOrderByCreatedAtDesc() } returns listOf(systemFrame("템플릿"))
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = service.listSystemFrames()

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("템플릿")
            assertThat(result[0].isSystem).isTrue()
        }
    }

    @Nested
    inner class UpdateSystemFrame {

        @Test
        @DisplayName("존재하지 않으면 SYSTEM_FRAME_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { frameRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.updateSystemFrame(1L, request()) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(FrameErrorCode.SYSTEM_FRAME_NOT_FOUND)
        }

        @Test
        @DisplayName("사용자 프레임이면 SYSTEM_FRAME_NOT_FOUND 예외를 던진다(방어)")
        fun notSystemFrame() {
            every { frameRepository.findById(1L) } returns Optional.of(userFrame())

            assertThatThrownBy { service.updateSystemFrame(1L, request()) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(FrameErrorCode.SYSTEM_FRAME_NOT_FOUND)
        }

        @Test
        @DisplayName("메타데이터를 갱신하고 교체된 프리뷰 자산을 정리한다")
        fun success() {
            val frame = systemFrame(title = "old", previewKey = "old-preview.png")
            every { frameRepository.findById(1L) } returns Optional.of(frame)
            every { frameAssetManager.normalizeKey("new-preview.png") } returns "new-preview.png"
            every { frameAssetManager.normalizeComponentKeys(any()) } returns emptyMap()
            every { frameAssetManager.deleteFiles(any()) } just Runs

            service.updateSystemFrame(1L, request(title = "new", previewKey = "new-preview.png"))

            assertThat(frame.title).isEqualTo("new")
            assertThat(frame.previewKey).isEqualTo("new-preview.png")
            verify { frameAssetManager.deleteFiles(listOf("old-preview.png")) }
        }
    }

    @Nested
    inner class DeleteSystemFrame {

        @Test
        @DisplayName("시스템 프레임의 자산을 정리하고 삭제한다")
        fun success() {
            val frame = systemFrame(previewKey = "preview.png")
            every { frameRepository.findById(1L) } returns Optional.of(frame)
            val deleted = slot<List<String?>>()
            every { frameAssetManager.deleteFiles(capture(deleted)) } just Runs
            every { frameRepository.delete(frame) } just Runs

            service.deleteSystemFrame(1L)

            assertThat(deleted.captured).contains("preview.png")
            verify { frameRepository.delete(frame) }
        }

        @Test
        @DisplayName("사용자 프레임이면 SYSTEM_FRAME_NOT_FOUND 예외를 던지고 삭제하지 않는다(방어)")
        fun notSystemFrame() {
            every { frameRepository.findById(1L) } returns Optional.of(userFrame())

            assertThatThrownBy { service.deleteSystemFrame(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(FrameErrorCode.SYSTEM_FRAME_NOT_FOUND)

            verify(exactly = 0) { frameRepository.delete(any()) }
        }
    }
}
