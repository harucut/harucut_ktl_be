package com.harucut.notice.dto

import com.harucut.notice.entity.Notice
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "공지 생성 요청")
data class CreateNoticeRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,

    @field:NotBlank(message = "본문은 필수입니다.")
    @Schema(description = "공지 본문", example = "안정적인 서비스 제공을 위해 점검을 진행합니다.")
    val content: String,

    @Schema(description = "상단 고정 여부", example = "false")
    val pinned: Boolean = false
)

@Schema(description = "공지 수정 요청")
data class UpdateNoticeRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,

    @field:NotBlank(message = "본문은 필수입니다.")
    @Schema(description = "공지 본문", example = "안정적인 서비스 제공을 위해 점검을 진행합니다.")
    val content: String,

    @Schema(description = "상단 고정 여부", example = "false")
    val pinned: Boolean = false
)

@Schema(description = "공지 응답 (게시된 공지만)")
data class NoticeResponse(
    @Schema(description = "공지 공개 ID", example = "aB3dE7fG9h")
    val publicId: String,
    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,
    @Schema(description = "공지 본문", example = "안정적인 서비스 제공을 위해 점검을 진행합니다.")
    val content: String,
    @Schema(description = "상단 고정 여부", example = "false")
    val pinned: Boolean,
    @Schema(description = "게시 일시", example = "2026-07-22T10:00:00")
    val publishedAt: LocalDateTime?
) {
    companion object {
        fun from(notice: Notice) = NoticeResponse(
            publicId = notice.publicId,
            title = notice.title,
            content = notice.content,
            pinned = notice.pinned,
            publishedAt = notice.publishedAt
        )
    }
}

@Schema(description = "관리자 공지 응답 (미게시 포함)")
data class NoticeAdminResponse(
    @Schema(description = "공지 ID", example = "1")
    val noticeId: Long,
    @Schema(description = "공지 공개 ID", example = "aB3dE7fG9h")
    val publicId: String,
    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,
    @Schema(description = "공지 본문", example = "안정적인 서비스 제공을 위해 점검을 진행합니다.")
    val content: String,
    @Schema(description = "상단 고정 여부", example = "false")
    val pinned: Boolean,
    @Schema(description = "게시 여부", example = "false")
    val published: Boolean,
    @Schema(description = "게시 일시", example = "2026-07-22T10:00:00")
    val publishedAt: LocalDateTime?
) {
    companion object {
        fun from(notice: Notice) = NoticeAdminResponse(
            noticeId = notice.id!!,
            publicId = notice.publicId,
            title = notice.title,
            content = notice.content,
            pinned = notice.pinned,
            published = notice.published,
            publishedAt = notice.publishedAt
        )
    }
}
