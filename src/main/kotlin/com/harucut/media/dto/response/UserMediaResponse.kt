package com.harucut.media.dto.response

import com.harucut.media.enums.UserMediaType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사용자 미디어 응답")
data class UserMediaResponse(
    @Schema(description = "미디어 ID")
    val mediaId: Long,

    @Schema(description = "미디어 타입", example = "VIDEO")
    val mediaType: UserMediaType,

    @Schema(description = "S3 Key")
    val s3Key: String,

    @Schema(description = "사용자 표시 파일명")
    val displayName: String,

    @Schema(description = "다운로드 URL (Presigned URL)")
    val downloadUrl: String?,

    @Schema(description = "원본 S3 Key")
    val originalS3Key: String?,

    @Schema(description = "원본 파일명")
    val originalFileName: String?,

    @Schema(description = "변환 작업 ID")
    val transcodeJobId: String?,

    @Schema(description = "등록 시각")
    val createdAt: LocalDateTime?
)
