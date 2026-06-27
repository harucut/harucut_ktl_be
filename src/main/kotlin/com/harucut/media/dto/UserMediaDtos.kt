package com.harucut.media.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "사용자 미디어 등록 요청")
data class UserMediaRegisterRequest(

    @field:NotBlank(message = "S3 Key는 필수입니다.")
    @Schema(
        description = "S3 Object Key",
        example = "uploads/users/AbCdEf12Gh/fourcuts/550e8400-e29b-41d4-a716-446655440000.png"
    )
    val s3Key: String,

    @Schema(description = "사용자 표시 파일명", example = "나의 기록.png")
    val displayName: String? = null
)

@Schema(description = "사용자 미디어 표시 파일명 수정 요청")
data class UserMediaDisplayNameUpdateRequest(

    @field:NotBlank(message = "표시 파일명은 필수입니다.")
    @field:Size(max = 255, message = "표시 파일명은 255자 이하여야 합니다.")
    @Schema(description = "사용자에게 표시될 파일명", example = "my_holiday_video")
    val displayName: String
)

@Schema(description = "사용자 미디어 응답")
data class UserMediaResponse(

    @Schema(description = "미디어 ID", example = "1")
    val mediaId: Long?,

    @Schema(description = "S3 Key", example = "uploads/users/AbCdEf12Gh/fourcuts/550e8400-...png")
    val s3Key: String,

    @Schema(description = "사용자 표시 파일명", example = "harucut_20260318_102030.png")
    val displayName: String,

    @Schema(description = "다운로드 URL")
    val downloadUrl: String?,

    @Schema(description = "등록 시각")
    val createdAt: LocalDateTime
)