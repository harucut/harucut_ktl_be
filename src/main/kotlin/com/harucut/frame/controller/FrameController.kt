package com.harucut.frame.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.service.FrameService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Frame", description = "프레임 생성/조회/수정/삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/user")
class FrameController(
    private val frameService: FrameService
) {

    // 프레임 생성
    @Operation(summary = "프레임 생성", description = "사용자 프레임을 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "보관 가능한 프레임 수 초과")
    )
    @PostMapping("/frame")
    fun createFrame(
        @RequestBody @Valid request: FrameCreateRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        frameService.createFrame(principal.id!!, request)
        return Response.ok().toResponseEntity()
    }

    // 내 프레임 목록 조회
    @Operation(summary = "내 프레임 목록 조회", description = "현재 로그인한 사용자의 프레임 목록을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @GetMapping("/frame")
    fun getMyFrames(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<List<FrameResponse>>> {
        val response = frameService.getMyFrames(principal.id!!)
        return Response.ok(response).toResponseEntity()
    }

    // 프레임 단건 조회
    @Operation(summary = "프레임 단건 조회", description = "프레임 ID로 단건 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "권한 없음 / 보관 기간 만료"),
        ApiResponse(responseCode = "404", description = "프레임을 찾을 수 없음")
    )
    @GetMapping("/frame/{frameId}")
    fun getFrame(
        @Parameter(description = "프레임 ID", required = true) @PathVariable frameId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<FrameResponse>> {
        val response = frameService.getFrame(frameId, principal.id!!)
        return Response.ok(response).toResponseEntity()
    }

    // 프레임 수정
    @Operation(summary = "프레임 수정", description = "프레임 ID로 기존 프레임을 수정합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "권한 없음"),
        ApiResponse(responseCode = "404", description = "프레임을 찾을 수 없음")
    )
    @PutMapping("/frame/{frameId}")
    fun updateFrame(
        @Parameter(description = "프레임 ID", required = true) @PathVariable frameId: Long,
        @RequestBody @Valid request: FrameCreateRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        frameService.updateFrame(principal.id!!, frameId, request)
        return Response.ok().toResponseEntity()
    }

    // 프레임 삭제
    @Operation(summary = "프레임 삭제", description = "프레임 ID로 프레임을 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "권한 없음"),
        ApiResponse(responseCode = "404", description = "프레임을 찾을 수 없음")
    )
    @DeleteMapping("/frame/{frameId}")
    fun deleteFrame(
        @Parameter(description = "프레임 ID", required = true) @PathVariable frameId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        frameService.deleteFrame(principal.id!!, frameId)
        return Response.ok().toResponseEntity()
    }
}