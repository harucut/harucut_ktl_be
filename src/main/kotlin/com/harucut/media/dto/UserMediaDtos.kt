package com.harucut.media.dto

import com.harucut.media.enums.UserMediaType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "사용자 미디어 등록 요청")
data class UserMediaRegisterRequest(

    @field:NotNull(message = "미디어 타입은 필수입니다.")
    @Schema(description = "미디어 타입", example = "PHOTO")
    val mediaType: UserMediaType,

    @field:NotBlank(message = "S3 Key는 필수입니다.")
    @Schema(
        description = "S3 Object Key",
        example = "uploads/users/AbCdEf12Gh/fourcuts/550e8400-e29b-41d4-a716-446655440000.png"
    )
    val s3Key: String,

    @Schema(description = "사용자 표시 파일명", example = "나의 기록.mp4")
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

    @Schema(description = "미디어 타입", example = "VIDEO")
    val mediaType: UserMediaType,

    @Schema(description = "S3 Key", example = "uploads/users/AbCdEf12Gh/mp4/550e8400-...mp4")
    val s3Key: String,

    @Schema(description = "사용자 표시 파일명", example = "harucut_20260318_102030.mp4")
    val displayName: String,

    @Schema(description = "다운로드 URL (사진만 즉시 제공, 영상은 null)")
    val downloadUrl: String?,

    @Schema(description = "썸네일 조회 URL (영상 포스터, 없으면 null)")
    val thumbnailUrl: String?,

    @Schema(description = "원본 S3 Key")
    val originalS3Key: String?,

    @Schema(description = "원본 파일명")
    val originalFileName: String?,

    @Schema(description = "변환 작업 ID")
    val transcodeJobId: String?,

    @Schema(description = "등록 시각")
    val createdAt: LocalDateTime
)