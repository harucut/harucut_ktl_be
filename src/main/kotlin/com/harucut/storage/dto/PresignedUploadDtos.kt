package com.harucut.storage.dto

import com.harucut.storage.enums.ContentType
import com.harucut.storage.enums.UploadType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.Duration

@Schema(description = "Presigned URL 생성 요청 DTO")
data class PresignedUploadRequest(

    @Schema(description = "업로드 타입 (PROFILE, FRAME 등)", example = "PROFILE")
    val type: UploadType,

    @field:NotBlank(message = "파일명은 필수입니다.")
    @Schema(description = "원본 파일명 (확장자 포함)", example = "profile_image.png")
    val filename: String,

    @Schema(description = "Content-Type, 타입만 대문자로 전송하며 확장자와 일치해야 함", example = "PNG")
    val contentType: ContentType
)

@Schema(description = "Presigned URL 생성 응답 DTO")
data class PresignedUploadResponse(

    @Schema(
        description = "S3에 저장될 키 (경로 포함)",
        example = "uploads/users/publicId/profile/550e8400-e29b-41d4-a716-446655440000.png"
    )
    val key: String,

    @Schema(
        description = "파일 업로드를 수행할 Presigned URL",
        example = "https://harucut-bucket.s3.ap-northeast-2.amazonaws.com/..."
    )
    val uploadUrl: String,

    @Schema(description = "파일 타입", example = "image/png")
    val contentType: String,

    @Schema(description = "URL 유효 기간", example = "PT24H")
    val expiresIn: Duration
)