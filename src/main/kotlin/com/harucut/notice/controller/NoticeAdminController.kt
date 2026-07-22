package com.harucut.notice.controller

import com.harucut.notice.dto.CreateNoticeRequest
import com.harucut.notice.dto.NoticeAdminResponse
import com.harucut.notice.dto.UpdateNoticeRequest
import com.harucut.notice.service.NoticeAdminService
import com.harucut.util.response.PageResponse
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Notice Admin", description = "관리자 공지사항 CRUD API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/notices")
class NoticeAdminController(
    private val noticeAdminService: NoticeAdminService
) {

    @Operation(summary = "공지 생성", description = "새 공지를 생성합니다. 생성 직후에는 미게시 상태입니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @PostMapping
    fun createNotice(@RequestBody @Valid request: CreateNoticeRequest): ResponseEntity<Response<Unit>> {
        noticeAdminService.createNotice(request.title, request.content, request.pinned)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "공지 수정", description = "공지의 제목/본문/상단고정 여부를 수정합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 공지")
    )
    @PatchMapping("/{noticeId}")
    fun updateNotice(
        @Parameter(description = "공지 ID", required = true) @PathVariable noticeId: Long,
        @RequestBody @Valid request: UpdateNoticeRequest
    ): ResponseEntity<Response<Unit>> {
        noticeAdminService.updateNotice(noticeId, request.title, request.content, request.pinned)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "공지 게시", description = "공지를 게시 상태로 전환합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "게시 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 공지")
    )
    @PatchMapping("/{noticeId}/publish")
    fun publishNotice(
        @Parameter(description = "공지 ID", required = true) @PathVariable noticeId: Long
    ): ResponseEntity<Response<Unit>> {
        noticeAdminService.publishNotice(noticeId)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "공지 게시 취소", description = "공지를 미게시 상태로 전환합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "게시 취소 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 공지")
    )
    @PatchMapping("/{noticeId}/unpublish")
    fun unpublishNotice(
        @Parameter(description = "공지 ID", required = true) @PathVariable noticeId: Long
    ): ResponseEntity<Response<Unit>> {
        noticeAdminService.unpublishNotice(noticeId)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "공지 삭제", description = "공지를 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 공지")
    )
    @DeleteMapping("/{noticeId}")
    fun deleteNotice(
        @Parameter(description = "공지 ID", required = true) @PathVariable noticeId: Long
    ): ResponseEntity<Response<Unit>> {
        noticeAdminService.deleteNotice(noticeId)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "전체 공지 목록 조회", description = "미게시 공지를 포함한 전체 공지 목록을 페이지 단위로 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 페이지 파라미터"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @GetMapping
    fun getAllNotices(
        @Parameter(description = "페이지 번호(0부터 시작)", schema = Schema(defaultValue = "0", minimum = "0"))
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본값은 10입니다.", schema = Schema(defaultValue = "10", minimum = "1"))
        @RequestParam(value = "size", defaultValue = "10") size: Int
    ): ResponseEntity<Response<PageResponse<NoticeAdminResponse>>> {
        return Response.ok(noticeAdminService.listAllNotices(page, size)).toResponseEntity()
    }
}
