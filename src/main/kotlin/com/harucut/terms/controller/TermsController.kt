package com.harucut.terms.controller

import com.harucut.terms.dto.TermsResponse
import com.harucut.terms.service.TermsService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Terms", description = "약관 조회 API")
@RestController
@RequestMapping("/api/terms")
class TermsController(
    private val termsService: TermsService
) {

    @Operation(
        summary = "활성 약관 목록 조회",
        description = "현재 활성화된 약관의 코드별 최신 버전(본문 포함) 목록을 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping
    fun getTerms(): ResponseEntity<Response<List<TermsResponse>>> {
        return Response.ok(termsService.getActiveTerms()).toResponseEntity()
    }
}
