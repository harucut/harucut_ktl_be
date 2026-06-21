package com.harucut.storage.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.storage.dto.PresignedUploadResponse
import com.harucut.storage.enums.ContentType
import com.harucut.storage.enums.UploadType
import com.harucut.storage.exception.StorageErrorCode
import com.harucut.storage.property.AwsProperties
import com.harucut.storage.strategy.UploadPathStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.nio.charset.StandardCharsets
import java.time.Duration

@Service
class S3FileStorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    awsProperties: AwsProperties,
    strategies: List<UploadPathStrategy>
) : FileStorageService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val bucketName: String = awsProperties.s3.bucket
    private val strategyMap: Map<UploadType, UploadPathStrategy> =
        strategies.associateBy { it.uploadType }

    override fun delete(key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucketName).key(key).build()
        )
    }

    override fun generatePresignedGetUrl(key: String): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(EXPIRY)
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    override fun generatePresignedDownloadUrl(key: String, downloadFileName: String?): String {
        val filename = if (!downloadFileName.isNullOrBlank()) {
            sanitizeFilename(downloadFileName)
        } else {
            extractFilenameFromKey(key)
        }
        val contentDisposition = buildContentDisposition(filename)

        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .responseContentDisposition(contentDisposition)
            .responseContentType("application/octet-stream")
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(EXPIRY)
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    override fun generatePresignedUploadUrl(
        uploadType: UploadType,
        originalFilename: String,
        contentType: ContentType,
        publicId: String,
        isTemp: Boolean
    ): PresignedUploadResponse {
        val extension = extractExtension(originalFilename)
        val validatedContentType = ContentType.validate(contentType.mimeType, extension)

        val strategy = strategyMap[uploadType]
            ?: throw BusinessException(
                StorageErrorCode.UNSUPPORTED_UPLOAD_TYPE,
                "${StorageErrorCode.UNSUPPORTED_UPLOAD_TYPE.message}: ${uploadType.name}"
            )

        val key = strategy.generateKey(publicId, originalFilename, isTemp)

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(validatedContentType.mimeType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(EXPIRY)
            .putObjectRequest(putObjectRequest)
            .build()

        val url = s3Presigner.presignPutObject(presignRequest).url().toString()

        return PresignedUploadResponse(key, url, validatedContentType.mimeType, EXPIRY)
    }

    override fun moveFile(sourceKey: String, destinationKey: String): String {
        if (sourceKey.isBlank() || destinationKey.isBlank()) {
            throw BusinessException(
                GlobalErrorCode.INVALID_INPUT_VALUE,
                "Source Key 또는 Destination Key가 비어있습니다."
            )
        }

        try {
            s3Client.copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(bucketName).sourceKey(sourceKey)
                    .destinationBucket(bucketName).destinationKey(destinationKey)
                    .build()
            )
            s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucketName).key(sourceKey).build()
            )

            return destinationKey
        } catch (e: S3Exception) {
            if (e.statusCode() == 404 || e.awsErrorDetails()?.errorCode() == "NoSuchKey") {
                log.warn("Temp file not found (Expired?): {}", sourceKey)
                throw BusinessException(
                    GlobalErrorCode.FILE_EXPIRED,
                    "임시 파일이 만료되었습니다. 이미지를 다시 업로드해주세요."
                )
            }
            log.error("AWS S3 Error during move: {} -> {}", sourceKey, destinationKey, e)
            throw BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 이동 실패")
        } catch (e: Exception) {
            log.error("Unexpected error during file move", e)
            throw BusinessException(
                GlobalErrorCode.INTERNAL_SERVER_ERROR,
                "파일 이동 중 알 수 없는 오류가 발생했습니다."
            )
        }
    }

    // ── helpers ──
    private fun extractExtension(filename: String?): String {
        if (filename == null) return ""
        val idx = filename.lastIndexOf('.')
        return if (idx == -1 || idx == filename.length - 1) "" else filename.substring(idx + 1)
    }

    private fun extractFilenameFromKey(key: String): String {
        if (key.isBlank()) return "file"
        val idx = key.lastIndexOf('/')
        val filename = if (idx in 0 until key.length - 1) key.substring(idx + 1) else key
        return sanitizeFilename(filename)
    }

    private fun sanitizeFilename(filename: String): String {
        if (filename.isBlank()) return "file"
        val sanitized = filename
            .replace("\\", "")
            .replace("/", "_")
            .replace("\"", "")
            .replace(";", "_")
            .replace(":", "_")
            .replace("\r", "")
            .replace("\n", "")
            .trim()
        return sanitized.ifEmpty { "file" }
    }

    private fun buildContentDisposition(filename: String): String {
        val asciiFallback = buildAsciiFallbackFilename(filename)
        val encodedUtf8Name = encodeRfc5987Value(filename)
        return "attachment; filename=\"$asciiFallback\"; filename*=UTF-8''$encodedUtf8Name"
    }

    private fun buildAsciiFallbackFilename(filename: String): String {
        val dotIndex = filename.lastIndexOf('.')
        var base = filename
        var extension = ""

        if (dotIndex in 1 until filename.length - 1) {
            base = filename.substring(0, dotIndex)
            extension = filename.substring(dotIndex)
        }

        var asciiBase = base
            .replace(Regex("[^\\x20-\\x7E]"), "_")
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (asciiBase.isBlank()) asciiBase = "download"

        var asciiExtension = extension.replace(Regex("[^A-Za-z0-9.]"), "")
        if (asciiExtension == ".") asciiExtension = ""

        return asciiBase + asciiExtension
    }

    private fun encodeRfc5987Value(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val encoded = StringBuilder(bytes.size * 3)

        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (isRfc5987AttrChar(c)) {
                encoded.append(c.toChar())
            } else {
                encoded.append('%')
                encoded.append(toHexUpper(c shr 4))
                encoded.append(toHexUpper(c))
            }
        }
        return encoded.toString()
    }

    private fun isRfc5987AttrChar(c: Int): Boolean =
        c in '0'.code..'9'.code ||
                c in 'A'.code..'Z'.code ||
                c in 'a'.code..'z'.code ||
                c.toChar() in "!#$&+-.^_`|~"

    private fun toHexUpper(value: Int): Char {
        val nibble = value and 0x0F
        return if (nibble < 10) ('0' + nibble) else ('A' + nibble - 10)
    }

    companion object {
        private val EXPIRY: Duration = Duration.ofDays(1)
    }
}