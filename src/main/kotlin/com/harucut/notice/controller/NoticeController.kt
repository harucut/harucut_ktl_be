package com.harucut.notice.controller

import com.harucut.notice.dto.NoticeResponse
import com.harucut.notice.service.NoticeService
import com.harucut.util.response.PageResponse
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Notice", description = "공지사항 조회 API")
@RestController
@RequestMapping("/api/notices")
class NoticeController(
    private val noticeService: NoticeService
) {

    @Operation(summary = "게시된 공지 목록 조회", description = "게시된 공지 목록을 상단고정 우선, 게시 최신순으로 페이지 단위 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 페이지 파라미터")
    )
    @GetMapping
    fun getNotices(
        @Parameter(description = "페이지 번호(0부터 시작)", schema = Schema(defaultValue = "0", minimum = "0"))
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본값은 10입니다.", schema = Schema(defaultValue = "10", minimum = "1"))
        @RequestParam(value = "size", defaultValue = "10") size: Int
    ): ResponseEntity<Response<PageResponse<NoticeResponse>>> {
        return Response.ok(noticeService.getPublishedNotices(page, size)).toResponseEntity()
    }

    @Operation(summary = "게시된 공지 단건 조회", description = "공개 ID로 게시된 공지 하나를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "존재하지 않거나 게시되지 않은 공지")
    )
    @GetMapping("/{publicId}")
    fun getNotice(
        @Parameter(description = "공지 공개 ID", required = true) @PathVariable publicId: String
    ): ResponseEntity<Response<NoticeResponse>> {
        return Response.ok(noticeService.getPublishedNotice(publicId)).toResponseEntity()
    }
}
