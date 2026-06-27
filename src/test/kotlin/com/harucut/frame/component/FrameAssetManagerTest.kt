package com.harucut.frame.component

import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.storage.service.FileStorageService
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FrameAssetManagerTest {

    private val fileStorageService = mockk<FileStorageService>()
    private val assetManager = FrameAssetManager(fileStorageService)

    private fun component(source: String) =
        FrameCreateRequest.ComponentRequest(type = ComponentType.PHOTO, source = source)

    @Nested
    inner class NormalizeComponentKeys {

        @Test
        @DisplayName("presigned URL로 들어온 uploads 경로를 순수 key로 정규화해 매핑을 반환한다")
        fun normalizesUrl() {
            val result = assetManager.normalizeComponentKeys(
                listOf(component("https://bucket.s3.amazonaws.com/uploads/users/pub/components/a.png"))
            )

            assertThat(result["https://bucket.s3.amazonaws.com/uploads/users/pub/components/a.png"])
                .isEqualTo("uploads/users/pub/components/a.png")
        }

        @Test
        @DisplayName("이미 순수 key면 매핑에 포함하지 않는다")
        fun alreadyNormalized() {
            val result = assetManager.normalizeComponentKeys(
                listOf(component("uploads/users/pub/components/b.png"))
            )

            assertThat(result).doesNotContainKey("uploads/users/pub/components/b.png")
        }

        @Test
        @DisplayName("컴포넌트가 비어있으면 빈 맵을 반환한다")
        fun emptyComponents() {
            assertThat(assetManager.normalizeComponentKeys(emptyList())).isEmpty()
            assertThat(assetManager.normalizeComponentKeys(null)).isEmpty()
        }
    }

    @Nested
    inner class NormalizeKey {

        @Test
        @DisplayName("presigned URL을 순수 key로 정규화한다")
        fun normalizesUrl() {
            val result = assetManager.normalizeKey("https://bucket.s3.amazonaws.com/uploads/users/pub/preview.png")

            assertThat(result).isEqualTo("uploads/users/pub/preview.png")
        }

        @Test
        @DisplayName("key가 null이면 null을 반환한다")
        fun nullKey() {
            assertThat(assetManager.normalizeKey(null)).isNull()
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
