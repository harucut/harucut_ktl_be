package com.harucut.media.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.media.entity.UserMedia
import com.harucut.media.enums.UserMediaType
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.media.dto.UserMediaDisplayNameUpdateRequest
import com.harucut.media.dto.UserMediaRegisterRequest
import com.harucut.media.repository.UserMediaRepository
import com.harucut.storage.service.FileStorageService
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

class UserMediaServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userMediaRepository = mockk<UserMediaRepository>()
    private val fileStorageService = mockk<FileStorageService>()
    private val subscriptionPolicy = mockk<MediaSubscriptionPolicy>(relaxed = true)

    private val service = UserMediaServiceImpl(
        userRepository, userMediaRepository, fileStorageService, subscriptionPolicy
    )

    private fun userMock(id: Long = 1L): User {
        val u = mockk<User>()
        every { u.id } returns id
        return u
    }

    @Nested
    inner class RegisterMedia {

        @Test
        @DisplayName("새 사진 등록 시 정규화된 key와 표시명으로 저장하고 응답을 반환한다")
        fun newPhoto() {
            // given
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByS3Key("uploads/users/pub/fourcuts/x.png") } returns null
            val saved = slot<UserMedia>()
            every { userMediaRepository.save(capture(saved)) } answers { saved.captured }
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            // when
            val res = service.registerMedia(
                1L, UserMediaRegisterRequest(UserMediaType.PHOTO, "uploads/users/pub/fourcuts/x.png", "내 사진")
            )

            // then
            assertThat(res.mediaType).isEqualTo(UserMediaType.PHOTO)
            assertThat(res.displayName).isEqualTo("내 사진.png")
            assertThat(res.downloadUrl).isEqualTo("https://dl")
            assertThat(saved.captured.s3Key).isEqualTo("uploads/users/pub/fourcuts/x.png")
        }

        @Test
        @DisplayName("전체 URL로 들어온 s3Key를 순수 key로 정규화해 저장한다")
        fun normalizeUrlKey() {
            // given
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByS3Key("uploads/users/pub/fourcuts/x.png") } returns null
            val saved = slot<UserMedia>()
            every { userMediaRepository.save(capture(saved)) } answers { saved.captured }
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            // when
            service.registerMedia(
                1L,
                UserMediaRegisterRequest(
                    UserMediaType.PHOTO,
                    "https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/users/pub/fourcuts/x.png",
                    "x"
                )
            )

            // then
            assertThat(saved.captured.s3Key).isEqualTo("uploads/users/pub/fourcuts/x.png")
        }

        @Test
        @DisplayName("표시명 미지정 시 harucut_타임스탬프 + key 확장자로 fallback 한다")
        fun fallbackDisplayName() {
            // given
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByS3Key(any()) } returns null
            every { userMediaRepository.save(any()) } answers { firstArg() }
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            // when
            val res = service.registerMedia(
                1L, UserMediaRegisterRequest(UserMediaType.PHOTO, "uploads/users/pub/fourcuts/x.png", null)
            )

            // then
            assertThat(res.displayName).matches("harucut_\\d{8}_\\d{6}\\.png")
        }

        @Test
        @DisplayName("동일 key가 같은 사용자 소유면 저장 없이 기존 항목을 반환한다(멱등)")
        fun idempotentSameOwner() {
            // given
            val user = userMock(1L)
            val existing = UserMedia.ofPhoto(user, "uploads/users/pub/fourcuts/x.png", "기존.png")
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByS3Key("uploads/users/pub/fourcuts/x.png") } returns existing
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            // when
            val res = service.registerMedia(
                1L, UserMediaRegisterRequest(UserMediaType.PHOTO, "uploads/users/pub/fourcuts/x.png", "무시됨")
            )

            // then
            assertThat(res.displayName).isEqualTo("기존.png")
            verify(exactly = 0) { userMediaRepository.save(any()) }
        }

        @Test
        @DisplayName("동일 key가 다른 사용자 소유면 FORBIDDEN 예외를 던진다")
        fun differentOwner() {
            // given
            val requester = userMock(1L)
            val owner = userMock(2L)
            val existing = UserMedia.ofPhoto(owner, "uploads/users/pub/fourcuts/x.png", "남의 것.png")
            every { userRepository.findById(1L) } returns Optional.of(requester)
            every { userMediaRepository.findByS3Key(any()) } returns existing

            // when & then
            assertThatThrownBy {
                service.registerMedia(
                    1L, UserMediaRegisterRequest(UserMediaType.PHOTO, "uploads/users/pub/fourcuts/x.png", "x")
                )
            }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN)
        }
    }

    @Nested
    inner class GetMyMedia {

        @Test
        @DisplayName("cutoff·타입이 없으면 전체 목록 finder를 호출한다")
        fun noCutoffNoType() {
            // given
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { subscriptionPolicy.resolveHistoryCutoff(user) } returns null
            val page = PageImpl(
                listOf(UserMedia.ofPhoto(user, "a/x.png", "x.png")),
                PageRequest.of(0, 10), 1
            )
            every { userMediaRepository.findAllByUserOrderByCreatedAtDesc(user, any()) } returns page
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            // when
            val res = service.getMyMedia(1L, null, 0, 10)

            // then
            assertThat(res.content).hasSize(1)
            assertThat(res.totalElements).isEqualTo(1)
            verify { userMediaRepository.findAllByUserOrderByCreatedAtDesc(user, any()) }
        }

        @Test
        @DisplayName("타입 필터가 있으면 타입 finder를 호출한다")
        fun withType() {
            // given
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { subscriptionPolicy.resolveHistoryCutoff(user) } returns null
            val page = PageImpl(
                listOf(UserMedia.ofVideo(user, "a/v.mp4", null, null, "v.mp4", "job", null)),
                PageRequest.of(0, 10), 1
            )
            every {
                userMediaRepository.findAllByUserAndMediaTypeOrderByCreatedAtDesc(user, UserMediaType.VIDEO, any())
            } returns page

            // when
            val res = service.getMyMedia(1L, UserMediaType.VIDEO, 0, 10)

            // then
            assertThat(res.content).hasSize(1)
            assertThat(res.content[0].downloadUrl).isNull()
            verify { userMediaRepository.findAllByUserAndMediaTypeOrderByCreatedAtDesc(user, UserMediaType.VIDEO, any()) }
        }

        @Test
        @DisplayName("보관 cutoff가 있으면 cutoff finder를 호출한다")
        fun withCutoff() {
            // given
            val user = userMock(1L)
            val cutoff = LocalDateTime.now().minusDays(3)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { subscriptionPolicy.resolveHistoryCutoff(user) } returns cutoff
            val page = PageImpl<UserMedia>(emptyList(), PageRequest.of(0, 10), 0)
            every {
                userMediaRepository.findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, cutoff, any())
            } returns page

            // when
            service.getMyMedia(1L, null, 0, 10)

            // then
            verify {
                userMediaRepository.findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, cutoff, any())
            }
        }

        @Test
        @DisplayName("page가 음수면 INVALID_INPUT_VALUE 예외를 던진다")
        fun negativePage() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)

            assertThatThrownBy { service.getMyMedia(1L, null, -1, 10) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
        }
    }

    @Nested
    inner class GetDownloadUrl {

        @Test
        @DisplayName("소유한 미디어면 다운로드 presigned URL을 반환한다")
        fun success() {
            val user = userMock(1L)
            val media = UserMedia.ofPhoto(user, "a/x.png", "x.png")
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByIdAndUser(5L, user) } returns media
            every { fileStorageService.generatePresignedDownloadUrl("a/x.png", "x.png") } returns "https://dl"

            val res = service.getDownloadUrl(1L, 5L)

            assertThat(res).isEqualTo("https://dl")
        }

        @Test
        @DisplayName("미디어가 없으면 NOT_FOUND 예외를 던진다")
        fun notFound() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByIdAndUser(5L, user) } returns null

            assertThatThrownBy { service.getDownloadUrl(1L, 5L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateDisplayName {

        @Test
        @DisplayName("표시명을 정규화해 반영하고 응답을 반환한다")
        fun success() {
            val user = userMock(1L)
            val media = UserMedia.ofPhoto(user, "a/x.png", "old.png")
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByIdAndUser(5L, user) } returns media
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            val res = service.updateDisplayName(1L, 5L, UserMediaDisplayNameUpdateRequest("새이름"))

            assertThat(res.displayName).isEqualTo("새이름.png")
            assertThat(media.displayName).isEqualTo("새이름.png")
        }

        @Test
        @DisplayName("255자를 초과하면 확장자를 보존하며 절단한다")
        fun truncateLongName() {
            val user = userMock(1L)
            val media = UserMedia.ofPhoto(user, "a/x.png", "old.png")
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userMediaRepository.findByIdAndUser(5L, user) } returns media
            every { fileStorageService.generatePresignedDownloadUrl(any(), any()) } returns "https://dl"

            val res = service.updateDisplayName(1L, 5L, UserMediaDisplayNameUpdateRequest("a".repeat(300)))

            assertThat(res.displayName).hasSize(255)
            assertThat(res.displayName).endsWith(".png")
        }
    }

    @Nested
    inner class SaveTranscodedVideo {

        @Test
        @DisplayName("userPublicId가 비어있으면 INVALID_INPUT_VALUE 예외를 던진다")
        fun blankPublicId() {
            assertThatThrownBy { service.saveTranscodedVideo("", null, "s3://b/out.mp4", null, "job") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
        }

        @Test
        @DisplayName("변환본을 저장하고 원본 key·썸네일 key·표시명을 채워 응답한다")
        fun success() {
            val user = userMock(1L)
            every { userRepository.findByPublicId("pub-1") } returns user
            every { userMediaRepository.findByS3Key("uploads/users/pub-1/mp4/v.mp4") } returns null
            val saved = slot<UserMedia>()
            every { userMediaRepository.save(capture(saved)) } answers { saved.captured }
            every { fileStorageService.generatePresignedGetUrl("uploads/users/pub-1/thumbnail/v.0000000.jpg") } returns "https://thumb"

            val res = service.saveTranscodedVideo(
                "pub-1",
                "v.webm",
                "s3://bucket/uploads/users/pub-1/mp4/v.mp4",
                "s3://bucket/uploads/users/pub-1/thumbnail/v.0000000.jpg",
                "job-1"
            )

            assertThat(res.mediaType).isEqualTo(UserMediaType.VIDEO)
            assertThat(res.displayName).isEqualTo("v.mp4")
            assertThat(res.downloadUrl).isNull()
            assertThat(res.thumbnailUrl).isEqualTo("https://thumb")
            assertThat(saved.captured.s3Key).isEqualTo("uploads/users/pub-1/mp4/v.mp4")
            assertThat(saved.captured.originalS3Key).isEqualTo("uploads/users/pub-1/webm/v.webm")
            assertThat(saved.captured.thumbnailKey).isEqualTo("uploads/users/pub-1/thumbnail/v.0000000.jpg")
            assertThat(saved.captured.transcodeJobId).isEqualTo("job-1")
        }

        @Test
        @DisplayName("썸네일 경로가 없으면 thumbnailKey와 thumbnailUrl이 null 이다")
        fun noThumbnail() {
            val user = userMock(1L)
            every { userRepository.findByPublicId("pub-1") } returns user
            every { userMediaRepository.findByS3Key(any()) } returns null
            val saved = slot<UserMedia>()
            every { userMediaRepository.save(capture(saved)) } answers { saved.captured }

            val res = service.saveTranscodedVideo("pub-1", "v.webm", "s3://b/uploads/users/pub-1/mp4/v.mp4", null, "job-1")

            assertThat(saved.captured.thumbnailKey).isNull()
            assertThat(res.thumbnailUrl).isNull()
            verify(exactly = 0) { fileStorageService.generatePresignedGetUrl(any()) }
        }

        @Test
        @DisplayName("이미 저장된 변환본이면 저장 없이 기존 항목을 반환한다(멱등)")
        fun idempotent() {
            val user = userMock(1L)
            val existing = UserMedia.ofVideo(user, "uploads/users/pub-1/mp4/v.mp4", null, null, "v.mp4", "job-1", null)
            every { userRepository.findByPublicId("pub-1") } returns user
            every { userMediaRepository.findByS3Key("uploads/users/pub-1/mp4/v.mp4") } returns existing

            val res = service.saveTranscodedVideo("pub-1", "v.webm", "s3://b/uploads/users/pub-1/mp4/v.mp4", null, "job-1")

            assertThat(res.displayName).isEqualTo("v.mp4")
            verify(exactly = 0) { userMediaRepository.save(any()) }
        }
    }
}
