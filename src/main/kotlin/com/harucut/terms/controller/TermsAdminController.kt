package com.harucut.terms.controller

import com.harucut.terms.dto.CreateTermsRequest
import com.harucut.terms.dto.ReviseTermsRequest
import com.harucut.terms.dto.TermsAdminResponse
import com.harucut.terms.service.TermsAdminService
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Terms Admin", description = "관리자 약관 CRUD API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/terms")
class TermsAdminController(
    private val termsAdminService: TermsAdminService
) {

    @Operation(summary = "약관 생성", description = "새 약관을 생성합니다. 버전 1이 함께 생성됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "409", description = "이미 존재하는 약관 코드")
    )
    @PostMapping
    fun createTerms(@RequestBody @Valid request: CreateTermsRequest): ResponseEntity<Response<Unit>> {
        termsAdminService.createTerms(request.code, request.title, request.required, request.content)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "약관 개정", description = "기존 약관의 새 버전(현재 최신 버전 + 1)을 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "개정 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 약관")
    )
    @PostMapping("/{termsId}/versions")
    fun reviseTerms(
        @Parameter(description = "약관 ID", required = true) @PathVariable termsId: Long,
        @RequestBody @Valid request: ReviseTermsRequest
    ): ResponseEntity<Response<Unit>> {
        termsAdminService.reviseTerms(termsId, request.content)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "전체 약관 목록 조회", description = "비활성 약관을 포함한 전체 약관 목록을 현재 본문 및 최신 버전 번호와 함께 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @GetMapping
    fun getAllTerms(): ResponseEntity<Response<List<TermsAdminResponse>>> {
        return Response.ok(termsAdminService.listAllTerms()).toResponseEntity()
    }

    @Operation(summary = "약관 비활성화", description = "약관을 비활성화합니다. 이미 비활성 상태여도 성공합니다(멱등).")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "비활성화 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 약관")
    )
    @DeleteMapping("/{termsId}")
    fun deactivateTerms(
        @Parameter(description = "약관 ID", required = true) @PathVariable termsId: Long
    ): ResponseEntity<Response<Unit>> {
        termsAdminService.deactivateTerms(termsId)
        return Response.ok().toResponseEntity()
    }
}
