package com.harucut.util.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "페이지 응답")
data class PageResponse<T>(
    @Schema(description = "현재 페이지 데이터")
    val content: List<T>,

    @Schema(description = "전체 데이터 수", example = "42")
    val totalElements: Long,

    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,

    @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
    val number: Int,

    @Schema(description = "페이지 크기", example = "10")
    val size: Int,
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size,
            )
    }
}