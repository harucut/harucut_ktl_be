package com.harucut.frame.component

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.storage.service.FileStorageService
import com.harucut.user.entity.User
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FrameAssetManagerTest {

    private val fileStorageService = mockk<FileStorageService>()
    private val assetManager = FrameAssetManager(fileStorageService)

    // publicId "pub"를 가지는 사용자 목
    private fun userMock(publicId: String = "pub"): User =
        mockk<User>().also { every { it.publicId } returns publicId }

    // 컴포넌트 요청 DTO 헬퍼
    private fun component(source: String) =
        FrameCreateRequest.ComponentRequest(type = ComponentType.PHOTO, source = source)

    @Nested
    inner class MoveTempFilesToPermanent {

        @Test
        @DisplayName("사용자 temp 경로 컴포넌트를 uploads로 승격하고 원본→최종 매핑을 반환한다")
        fun promotesUserTemp() {
            val user = userMock()
            every {
                fileStorageService.moveFile(
                    "temp/users/pub/components/a.png",
                    "uploads/users/pub/components/a.png"
                )
            } returns "uploads/users/pub/components/a.png"

            val result = assetManager.moveTempFilesToPermanent(
                user, listOf(component("temp/users/pub/components/a.png"))
            )

            assertThat(result["temp/users/pub/components/a.png"]).isEqualTo("uploads/users/pub/components/a.png")
            verify {
                fileStorageService.moveFile(
                    "temp/users/pub/components/a.png",
                    "uploads/users/pub/components/a.png"
                )
            }
        }

        @Test
        @DisplayName("이미 영구(uploads) 경로면 이동하지 않는다")
        fun skipsPermanent() {
            val user = userMock()

            val result = assetManager.moveTempFilesToPermanent(
                user, listOf(component("uploads/users/pub/components/b.png"))
            )

            assertThat(result).doesNotContainKey("uploads/users/pub/components/b.png")
            verify(exactly = 0) { fileStorageService.moveFile(any(), any()) }
        }

        @Test
        @DisplayName("다른 사용자의 temp 경로는 이동하지 않는다")
        fun skipsOtherUserTemp() {
            val user = userMock()

            assetManager.moveTempFilesToPermanent(
                user, listOf(component("temp/users/other/components/c.png"))
            )

            verify(exactly = 0) { fileStorageService.moveFile(any(), any()) }
        }

        @Test
        @DisplayName("컴포넌트가 비어있으면 빈 맵을 반환한다")
        fun emptyComponents() {
            assertThat(assetManager.moveTempFilesToPermanent(userMock(), emptyList())).isEmpty()
            assertThat(assetManager.moveTempFilesToPermanent(userMock(), null)).isEmpty()
        }

        @Test
        @DisplayName("이동 중 예외가 발생하면 INTERNAL_SERVER_ERROR로 감싼다")
        fun moveFails() {
            val user = userMock()
            every { fileStorageService.moveFile(any(), any()) } throws RuntimeException("s3 down")

            assertThatThrownBy {
                assetManager.moveTempFilesToPermanent(user, listOf(component("temp/users/pub/components/a.png")))
            }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR)
        }
    }

    @Nested
    inner class MoveTempFileToPermanent {

        @Test
        @DisplayName("사용자 temp key를 uploads로 승격해 반환한다")
        fun promotes() {
            val user = userMock()
            every {
                fileStorageService.moveFile("temp/users/pub/preview.png", "uploads/users/pub/preview.png")
            } returns "uploads/users/pub/preview.png"

            val result = assetManager.moveTempFileToPermanent(user, "temp/users/pub/preview.png")

            assertThat(result).isEqualTo("uploads/users/pub/preview.png")
        }

        @Test
        @DisplayName("temp 경로가 아니면 정규화만 하고 이동하지 않는다")
        fun notTemp() {
            val user = userMock()

            val result = assetManager.moveTempFileToPermanent(user, "uploads/users/pub/preview.png")

            assertThat(result).isEqualTo("uploads/users/pub/preview.png")
            verify(exactly = 0) { fileStorageService.moveFile(any(), any()) }
        }

        @Test
        @DisplayName("key가 null이면 null을 반환한다")
        fun nullKey() {
            assertThat(assetManager.moveTempFileToPermanent(userMock(), null)).isNull()
        }
    }

    @Nested
    inner class ResolveSource {

        @Test
        @DisplayName("PHOTO이고 관리 경로면 presigned GET URL을 반환한다")
        fun photoManaged() {
            every { fileStorageService.generatePresignedGetUrl("uploads/users/pub/components/a.png") } returns "https://signed"

            val result = assetManager.resolveSource(ComponentType.PHOTO, "uploads/users/pub/components/a.png")

            assertThat(result).isEqualTo("https://signed")
        }

        @Test
        @DisplayName("STICKER는 관리 경로여도 presign하지 않고 원본을 반환한다")
        fun stickerPassThrough() {
            val result = assetManager.resolveSource(ComponentType.STICKER, "uploads/users/pub/components/a.png")

            assertThat(result).isEqualTo("uploads/users/pub/components/a.png")
            verify(exactly = 0) { fileStorageService.generatePresignedGetUrl(any()) }
        }

        @Test
        @DisplayName("관리 경로가 아닌 외부 URL은 원본을 그대로 반환한다")
        fun externalUrl() {
            val result = assetManager.resolveSource(ComponentType.PHOTO, "https://cdn.example.com/x.png")

            assertThat(result).isEqualTo("https://cdn.example.com/x.png")
            verify(exactly = 0) { fileStorageService.generatePresignedGetUrl(any()) }
        }

        @Test
        @DisplayName("배경 IMAGE이고 관리 경로면 presigned GET URL을 반환한다")
        fun backgroundImage() {
            every { fileStorageService.generatePresignedGetUrl("uploads/users/pub/bg.png") } returns "https://bg"

            val result = assetManager.resolveSource(BackgroundType.IMAGE, "uploads/users/pub/bg.png")

            assertThat(result).isEqualTo("https://bg")
        }

        @Test
        @DisplayName("배경 COLOR 값은 presign하지 않고 원본을 반환한다")
        fun backgroundColor() {
            val result = assetManager.resolveSource(BackgroundType.COLOR, "#ffffff")

            assertThat(result).isEqualTo("#ffffff")
            verify(exactly = 0) { fileStorageService.generatePresignedGetUrl(any()) }
        }
    }

    @Nested
    inner class DeleteFiles {

        @Test
        @DisplayName("유효한 key만 삭제하고 null/공백은 건너뛴다")
        fun deletesValidOnly() {
            every { fileStorageService.delete(any()) } just Runs

            assetManager.deleteFiles(listOf("uploads/a.png", null, "  ", "uploads/b.png"))

            verify(exactly = 1) { fileStorageService.delete("uploads/a.png") }
            verify(exactly = 1) { fileStorageService.delete("uploads/b.png") }
            verify(exactly = 2) { fileStorageService.delete(any()) }
        }
    }
}
