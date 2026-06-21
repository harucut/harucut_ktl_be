package com.harucut.storage.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.storage.enums.ContentType
import com.harucut.storage.enums.UploadType
import com.harucut.storage.exception.StorageErrorCode
import com.harucut.storage.property.AwsProperties
import com.harucut.storage.strategy.UploadPathStrategy
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI

class S3FileStorageServiceTest {

    private val s3Client = mockk<S3Client>(relaxed = true)
    private val s3Presigner = mockk<S3Presigner>()

    private val profileStrategy = object : UploadPathStrategy {
        override val uploadType = UploadType.PROFILE
        override fun generateKey(publicId: String, originalFilename: String, isTemp: Boolean) =
            "uploads/users/$publicId/profile/fixed.png"
    }

    private lateinit var service: FileStorageService

    companion object {
        private const val BUCKET = "harucut-test"
    }

    @BeforeEach
    fun setUp() {
        val properties = AwsProperties(
            region = "ap-northeast-2",
            credentials = AwsProperties.Credentials("ak", "sk"),
            s3 = AwsProperties.S3(BUCKET),
            mediaconvert = AwsProperties.MediaConvert("https://mc", "role-arn", "tmpl")
        )
        service = S3FileStorageService(s3Client, s3Presigner, properties, listOf(profileStrategy))
    }

    @Nested
    inner class GeneratePresignedUploadUrl {

        @Test
        @DisplayName("전략이 만든 key와 검증된 contentType으로 presigned PUT URL을 발급한다")
        fun success() {
            // given
            val presigned = mockk<PresignedPutObjectRequest>()
            every { presigned.url() } returns URI("https://x.s3/upload").toURL()
            val requestSlot = slot<PutObjectPresignRequest>()
            every { s3Presigner.presignPutObject(capture(requestSlot)) } returns presigned

            // when
            val result = service.generatePresignedUploadUrl(
                UploadType.PROFILE, "photo.png", ContentType.PNG, "pub-1", false
            )

            // then
            assertThat(result.key).isEqualTo("uploads/users/pub-1/profile/fixed.png")
            assertThat(result.contentType).isEqualTo("image/png")
            assertThat(result.uploadUrl).isEqualTo("https://x.s3/upload")

            val captured: PutObjectRequest = requestSlot.captured.putObjectRequest()
            assertThat(captured)
                .extracting("bucket", "key", "contentType")
                .containsExactly(BUCKET, "uploads/users/pub-1/profile/fixed.png", "image/png")
        }

        @Test
        @DisplayName("등록되지 않은 업로드 타입이면 UNSUPPORTED_UPLOAD_TYPE 예외를 던진다")
        fun unsupportedType() {
            assertThatThrownBy {
                service.generatePresignedUploadUrl(
                    UploadType.FRAME, "v.webm", ContentType.WEBM, "pub-1", false
                )
            }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(StorageErrorCode.UNSUPPORTED_UPLOAD_TYPE)
        }

        @Test
        @DisplayName("MIME과 확장자가 맞지 않으면 UNSUPPORTED_MEDIA_TYPE 예외를 던진다")
        fun mismatchedContentType() {
            assertThatThrownBy {
                service.generatePresignedUploadUrl(
                    UploadType.PROFILE, "photo.png", ContentType.JPEG, "pub-1", false
                )
            }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE)
        }
    }

    @Nested
    inner class GeneratePresignedGetUrl {

        @Test
        @DisplayName("key에 대한 presigned GET URL을 반환한다")
        fun success() {
            val presigned = mockk<PresignedGetObjectRequest>()
            every { presigned.url() } returns URI("https://x.s3/get").toURL()
            every { s3Presigner.presignGetObject(any<software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest>()) } returns presigned

            val result = service.generatePresignedGetUrl("uploads/a.png")

            assertThat(result).isEqualTo("https://x.s3/get")
        }
    }

    @Nested
    inner class Delete {

        @Test
        @DisplayName("버킷과 key로 S3 객체를 삭제한다")
        fun success() {
            every { s3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse.builder().build()
            val slot = slot<DeleteObjectRequest>()

            service.delete("uploads/a.png")

            verify { s3Client.deleteObject(capture(slot)) }
            assertThat(slot.captured)
                .extracting("bucket", "key")
                .containsExactly(BUCKET, "uploads/a.png")
        }
    }

    @Nested
    inner class MoveFile {

        @Test
        @DisplayName("원본을 복사한 뒤 원본을 삭제하고 목적지 key를 반환한다")
        fun success() {
            every { s3Client.copyObject(any<CopyObjectRequest>()) } returns CopyObjectResponse.builder().build()
            every { s3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse.builder().build()

            val result = service.moveFile("temp/a.png", "uploads/a.png")

            assertThat(result).isEqualTo("uploads/a.png")
            verify { s3Client.copyObject(any<CopyObjectRequest>()) }
            verify { s3Client.deleteObject(any<DeleteObjectRequest>()) }
        }

        @Test
        @DisplayName("source 또는 destination이 비어있으면 INVALID_INPUT_VALUE 예외를 던진다")
        fun blankKey() {
            assertThatThrownBy { service.moveFile("", "uploads/a.png") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
        }

        @Test
        @DisplayName("원본이 없으면(NoSuchKey) FILE_EXPIRED 예외를 던진다")
        fun expired() {
            val s3Exception = S3Exception.builder()
                .statusCode(404)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchKey").build())
                .build() as S3Exception
            every { s3Client.copyObject(any<CopyObjectRequest>()) } throws s3Exception

            assertThatThrownBy { service.moveFile("temp/a.png", "uploads/a.png") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FILE_EXPIRED)
        }
    }
}