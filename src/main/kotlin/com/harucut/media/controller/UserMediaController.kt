package com.harucut.media.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.media.dto.UserMediaDisplayNameUpdateRequest
import com.harucut.media.dto.UserMediaRegisterRequest
import com.harucut.media.dto.UserMediaResponse
import com.harucut.media.service.UserMediaService
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "User Media", description = "사용자 미디어(사진) 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/user/media")
class UserMediaController(
    private val userMediaService: UserMediaService
) {

    @Operation(summary = "내 미디어 등록", description = "S3 업로드 완료 후, key를 DB에 등록합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "등록 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "다른 사용자의 미디어 key")
    )
    @PostMapping
    fun registerMedia(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: UserMediaRegisterRequest
    ): ResponseEntity<Response<UserMediaResponse>> {
        val response = userMediaService.registerMedia(principal.id!!, request)
        return Response.ok(response).toResponseEntity()
    }

    @Operation(summary = "내 미디어 목록 조회", description = "사용자의 사진 목록과 다운로드 URL을 페이지 단위로 반환합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @GetMapping
    fun getMyMedia(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Parameter(description = "페이지 번호(0부터 시작)", schema = Schema(defaultValue = "0", minimum = "0"))
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본값은 10입니다.", schema = Schema(defaultValue = "10", minimum = "1"))
        @RequestParam(value = "size", defaultValue = "10") size: Int
    ): ResponseEntity<Response<PageResponse<UserMediaResponse>>> {
        val response = userMediaService.getMyMedia(principal.id!!, page, size)
        return Response.ok(response).toResponseEntity()
    }

    @Operation(summary = "미디어 다운로드 URL 조회", description = "다운로드 버튼 클릭 시 사용할 presigned URL을 반환합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "미디어를 찾을 수 없음")
    )
    @GetMapping("/{mediaId}/download-url")
    fun getDownloadUrl(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable("mediaId") mediaId: Long
    ): ResponseEntity<Response<String>> {
        val response = userMediaService.getDownloadUrl(principal.id!!, mediaId)
        return Response.ok(response).toResponseEntity()
    }

    @Operation(summary = "미디어 파일명 수정", description = "사용자에게 표시될 파일명을 수정합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "미디어를 찾을 수 없음")
    )
    @PatchMapping("/{mediaId}/display-name")
    fun updateDisplayName(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable("mediaId") mediaId: Long,
        @RequestBody @Valid request: UserMediaDisplayNameUpdateRequest
    ): ResponseEntity<Response<UserMediaResponse>> {
        val response = userMediaService.updateDisplayName(principal.id!!, mediaId, request)
        return Response.ok(response).toResponseEntity()
    }
}