package com.harucut.frame.controller

import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.service.FrameAdminService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Frame Admin", description = "관리자 기본 제공(시스템) 프레임 CRUD API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/frames")
class FrameAdminController(
    private val frameAdminService: FrameAdminService
) {

    @Operation(summary = "시스템 프레임 생성", description = "모든 사용자에게 노출되는 기본 제공 프레임을 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @PostMapping
    fun createFrame(@RequestBody @Valid request: FrameCreateRequest): ResponseEntity<Response<Unit>> {
        frameAdminService.createSystemFrame(request)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "시스템 프레임 수정", description = "시스템 프레임 ID로 메타데이터/컴포넌트를 수정합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 시스템 프레임")
    )
    @PatchMapping("/{frameId}")
    fun updateFrame(
        @Parameter(description = "프레임 ID", required = true) @PathVariable frameId: Long,
        @RequestBody @Valid request: FrameCreateRequest
    ): ResponseEntity<Response<Unit>> {
        frameAdminService.updateSystemFrame(frameId, request)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "시스템 프레임 삭제", description = "시스템 프레임 ID로 프레임을 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 시스템 프레임")
    )
    @DeleteMapping("/{frameId}")
    fun deleteFrame(
        @Parameter(description = "프레임 ID", required = true) @PathVariable frameId: Long
    ): ResponseEntity<Response<Unit>> {
        frameAdminService.deleteSystemFrame(frameId)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "시스템 프레임 목록 조회", description = "기본 제공 프레임 전체 목록을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @GetMapping
    fun getSystemFrames(): ResponseEntity<Response<List<FrameResponse>>> {
        return Response.ok(frameAdminService.listSystemFrames()).toResponseEntity()
    }
}
