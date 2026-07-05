package com.harucut.frame.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.attributes.BackgroundAttributes
import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.attributes.ImageBackgroundAttributes
import com.harucut.frame.component.FrameAssetManager
import com.harucut.frame.converter.FrameStyleConverter
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.entity.Frame
import com.harucut.frame.entity.FrameComponent
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.frame.enums.FrameType
import com.harucut.frame.policy.FrameSubscriptionPolicy
import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import com.harucut.util.entity.BaseEntity
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
import java.time.LocalDateTime
import java.util.Optional

class FrameServiceTest {

    private val frameRepository = mockk<FrameRepository>()
    private val userRepository = mockk<UserRepository>()
    private val frameAssetManager = mockk<FrameAssetManager>()
    private val frameStyleConverter = mockk<FrameStyleConverter>(relaxed = true)
    private val frameSubscriptionPolicy = mockk<FrameSubscriptionPolicy>()

    private val service = FrameServiceImpl(
        frameRepository, userRepository, frameAssetManager, frameStyleConverter, frameSubscriptionPolicy
    )

    private fun userMock(id: Long = 1L): User =
        mockk<User>(relaxed = true).also { every { it.id } returns id }

    private fun componentRequest(source: String, type: ComponentType = ComponentType.PHOTO) =
        FrameCreateRequest.ComponentRequest(type = type, source = source)

    private fun frame(
        user: User,
        title: String = "title",
        previewKey: String = "uploads/users/pub/preview.png",
        background: BackgroundAttributes = ColorBackgroundAttributes("#ffffff"),
        createdAt: LocalDateTime? = null
    ): Frame {
        val f = Frame(
            title = title,
            description = "desc",
            previewKey = previewKey,
            frameType = FrameType.CLASSIC,
            background = background,
            user = user
        )
        if (createdAt != null) setCreatedAt(f, createdAt)
        return f
    }

    private fun setCreatedAt(entity: BaseEntity, time: LocalDateTime) {
        val field = BaseEntity::class.java.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(entity, time)
    }

    @Nested
    inner class CreateFrame {

        @Test
        @DisplayName("보관 cap 검증 후 key를 정규화하고 컴포넌트와 함께 저장한다")
        fun success() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { frameRepository.countByUser(user) } returns 0L
            every { frameSubscriptionPolicy.assertFrameRetentionLimit(user, 0) } just Runs
            every { frameAssetManager.normalizeComponentKeys(any()) } returns emptyMap()
            every { frameAssetManager.normalizeKey("uploads/users/pub/preview.png") } returns "uploads/users/pub/preview.png"
            every { frameStyleConverter.convertToJson(any()) } returns "{}"
            val saved = slot<Frame>()
            every { frameRepository.save(capture(saved)) } answers { saved.captured }

            val request = FrameCreateRequest(
                title = "봄 여행 4컷",
                description = "벚꽃",
                previewKey = "uploads/users/pub/preview.png",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#ffffff"),
                components = listOf(componentRequest("uploads/users/pub/components/a.png"))
            )

            service.createFrame(1L, request)

            assertThat(saved.captured.previewKey).isEqualTo("uploads/users/pub/preview.png")
            assertThat(saved.captured.title).isEqualTo("봄 여행 4컷")
            assertThat(saved.captured.components).hasSize(1)
            verify { frameSubscriptionPolicy.assertFrameRetentionLimit(user, 0) }
        }

        @Test
        @DisplayName("보관 cap 초과면 PLAN_FRAME_RETENTION_EXCEEDED 예외를 던지고 저장하지 않는다")
        fun capExceeded() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { frameRepository.countByUser(user) } returns 1L
            every { frameSubscriptionPolicy.assertFrameRetentionLimit(user, 1) } throws
                BusinessException(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)

            val request = FrameCreateRequest(
                title = "t",
                description = null,
                previewKey = "uploads/users/pub/preview.png",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#ffffff"),
                components = null
            )

            assertThatThrownBy { service.createFrame(1L, request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)

            verify(exactly = 0) { frameAssetManager.normalizeComponentKeys(any()) }
            verify(exactly = 0) { frameRepository.save(any()) }
        }

        @Test
        @DisplayName("사용자가 없으면 NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            every { userRepository.findById(1L) } returns Optional.empty()

            val request = FrameCreateRequest(
                title = "t",
                description = null,
                previewKey = "p",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#ffffff"),
                components = null
            )

            assertThatThrownBy { service.createFrame(1L, request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }
    }

    @Nested
    inner class GetMyFrames {

        @Test
        @DisplayName("보관 cutoff 이전에 생성된 프레임은 제외하고 반환한다")
        fun filtersByCutoff() {
            val user = userMock(1L)
            val cutoff = LocalDateTime.now().minusDays(3)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { frameSubscriptionPolicy.resolveHistoryCutoff(user) } returns cutoff
            every { frameSubscriptionPolicy.resolveFrameRetentionCap(user) } returns null

            val recent = frame(user, title = "recent")
            val old = frame(user, title = "old", createdAt = LocalDateTime.now().minusDays(10))
            every { frameRepository.findAllByUserOrderByCreatedAtDesc(user) } returns listOf(recent, old)
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = service.getMyFrames(1L)

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("recent")
            assertThat(result[0].source).isEqualTo("preview-url")
        }

        @Test
        @DisplayName("cutoff가 null(무제한)이면 모든 프레임을 반환한다")
        fun unlimited() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { frameSubscriptionPolicy.resolveHistoryCutoff(user) } returns null
            every { frameSubscriptionPolicy.resolveFrameRetentionCap(user) } returns null

            val a = frame(user, title = "a")
            val b = frame(user, title = "b", createdAt = LocalDateTime.now().minusYears(5))
            every { frameRepository.findAllByUserOrderByCreatedAtDesc(user) } returns listOf(a, b)
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = service.getMyFrames(1L)

            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("동시 보관 cap을 초과한 프레임은 최신순으로 cap 개수만 반환한다")
        fun softCap() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { frameSubscriptionPolicy.resolveHistoryCutoff(user) } returns null
            every { frameSubscriptionPolicy.resolveFrameRetentionCap(user) } returns 1

            val newest = frame(user, title = "newest")
            val older = frame(user, title = "older", createdAt = LocalDateTime.now().minusDays(1))
            every { frameRepository.findAllByUserOrderByCreatedAtDesc(user) } returns listOf(newest, older)
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = service.getMyFrames(1L)

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("newest")
        }
    }

    @Nested
    inner class GetFrame {

        @Test
        @DisplayName("소유한 프레임이고 보관 기간 내면 응답을 반환한다")
        fun success() {
            val user = userMock(1L)
            val target = frame(user, title = "내 프레임")
            every { frameRepository.findById(10L) } returns Optional.of(target)
            every { frameSubscriptionPolicy.assertHistoryAccessible(any(), any()) } just Runs
            every { frameSubscriptionPolicy.resolveFrameRetentionCap(user) } returns null
            every { frameAssetManager.resolveSource(BackgroundType.IMAGE, any<String>()) } returns "preview-url"

            val result = service.getFrame(10L, 1L)

            assertThat(result.title).isEqualTo("내 프레임")
            assertThat(result.source).isEqualTo("preview-url")
        }

        @Test
        @DisplayName("동시 보관 cap 밖의 프레임이면 PLAN_FRAME_RETENTION_EXCEEDED 예외를 던진다")
        fun beyondRetentionCap() {
            val user = userMock(1L)
            val target = frame(user, createdAt = LocalDateTime.now().minusDays(1))
            every { frameRepository.findById(10L) } returns Optional.of(target)
            every { frameSubscriptionPolicy.assertHistoryAccessible(any(), any()) } just Runs
            every { frameSubscriptionPolicy.resolveFrameRetentionCap(user) } returns 1
            every { frameRepository.countByUserAndCreatedAtAfter(user, any()) } returns 1L

            assertThatThrownBy { service.getFrame(10L, 1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }

        @Test
        @DisplayName("프레임이 없으면 NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { frameRepository.findById(10L) } returns Optional.empty()

            assertThatThrownBy { service.getFrame(10L, 1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }

        @Test
        @DisplayName("다른 사용자의 프레임이면 FORBIDDEN 예외를 던진다")
        fun forbidden() {
            val owner = userMock(2L)
            val target = frame(owner)
            every { frameRepository.findById(10L) } returns Optional.of(target)

            assertThatThrownBy { service.getFrame(10L, 1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN)
        }

        @Test
        @DisplayName("보관 기간을 벗어난 프레임이면 정책 예외를 전파한다")
        fun historyExpired() {
            val user = userMock(1L)
            val target = frame(user)
            every { frameRepository.findById(10L) } returns Optional.of(target)
            every { frameSubscriptionPolicy.assertHistoryAccessible(any(), any()) } throws
                BusinessException(SubscriptionErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED)

            assertThatThrownBy { service.getFrame(10L, 1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED)
        }
    }

    @Nested
    inner class DeleteFrame {

        @Test
        @DisplayName("사진/배경/프리뷰 key를 정리하고 프레임을 삭제한다")
        fun success() {
            val user = userMock(1L)
            val target = frame(
                user,
                previewKey = "uploads/users/pub/preview.png",
                background = ImageBackgroundAttributes("uploads/users/pub/bg.png", 1.0)
            )
            target.addComponent(
                FrameComponent(
                    source = "uploads/users/pub/components/photo.png",
                    type = ComponentType.PHOTO,
                    x = 0.0, y = 0.0, width = null, height = null, scale = null,
                    rotation = 0.0, zIndex = 0, styleJson = null
                )
            )
            every { frameRepository.findById(10L) } returns Optional.of(target)
            val deleted = slot<List<String?>>()
            every { frameAssetManager.deleteFiles(capture(deleted)) } just Runs
            every { frameRepository.delete(target) } just Runs

            service.deleteFrame(1L, 10L)

            assertThat(deleted.captured).contains(
                "uploads/users/pub/components/photo.png",
                "uploads/users/pub/bg.png",
                "uploads/users/pub/preview.png"
            )
            verify { frameRepository.delete(target) }
        }

        @Test
        @DisplayName("다른 사용자의 프레임이면 FORBIDDEN 예외를 던지고 삭제하지 않는다")
        fun forbidden() {
            val owner = userMock(2L)
            val target = frame(owner)
            every { frameRepository.findById(10L) } returns Optional.of(target)

            assertThatThrownBy { service.deleteFrame(1L, 10L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN)

            verify(exactly = 0) { frameRepository.delete(any()) }
        }
    }

    @Nested
    inner class UpdateFrame {

        @Test
        @DisplayName("메타데이터·컴포넌트를 교체하고 교체된 배경/프리뷰/미사용 사진 자산을 정리한다")
        fun success() {
            val user = userMock(1L)
            val target = frame(
                user,
                title = "old",
                previewKey = "uploads/users/pub/old-preview.png",
                background = ImageBackgroundAttributes("uploads/users/pub/old-bg.png", 1.0)
            )
            target.addComponent(
                FrameComponent(
                    source = "uploads/users/pub/components/old.png",
                    type = ComponentType.PHOTO,
                    x = 0.0, y = 0.0, width = null, height = null, scale = null,
                    rotation = 0.0, zIndex = 0, styleJson = null
                )
            )
            every { frameRepository.findById(10L) } returns Optional.of(target)
            every { frameAssetManager.normalizeKey("uploads/users/pub/new-preview.png") } returns
                "uploads/users/pub/new-preview.png"
            every { frameAssetManager.normalizeComponentKeys(any()) } returns emptyMap()
            every { frameStyleConverter.convertToJson(any()) } returns "{}"
            every { frameAssetManager.deleteFiles(any()) } just Runs

            val request = FrameCreateRequest(
                title = "new",
                description = "n",
                previewKey = "uploads/users/pub/new-preview.png",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#000000"),
                components = listOf(componentRequest("uploads/users/pub/components/new.png"))
            )

            service.updateFrame(1L, 10L, request)

            assertThat(target.title).isEqualTo("new")
            assertThat(target.previewKey).isEqualTo("uploads/users/pub/new-preview.png")
            assertThat(target.background).isInstanceOf(ColorBackgroundAttributes::class.java)
            assertThat(target.components).hasSize(1)

            verify { frameAssetManager.deleteFiles(listOf("uploads/users/pub/old-bg.png")) }
            verify { frameAssetManager.deleteFiles(listOf("uploads/users/pub/old-preview.png")) }
            verify { frameAssetManager.deleteFiles(listOf("uploads/users/pub/components/old.png")) }
        }

        @Test
        @DisplayName("다른 사용자의 프레임이면 FORBIDDEN 예외를 던진다")
        fun forbidden() {
            val owner = userMock(2L)
            val target = frame(owner)
            every { frameRepository.findById(10L) } returns Optional.of(target)

            val request = FrameCreateRequest(
                title = "t",
                description = null,
                previewKey = "p",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#fff"),
                components = null
            )

            assertThatThrownBy { service.updateFrame(1L, 10L, request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN)
        }
    }
}
