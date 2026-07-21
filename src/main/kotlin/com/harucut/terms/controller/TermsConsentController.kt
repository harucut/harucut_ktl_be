package com.harucut.terms.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.terms.dto.ConsentItem
import com.harucut.terms.dto.TermsConsentStatusResponse
import com.harucut.terms.service.TermsService
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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Terms Consent", description = "내 약관 동의 조회/처리 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/auth/terms/consents")
class TermsConsentController(
    private val termsService: TermsService
) {

    @Operation(
        summary = "내 약관 동의 상태 조회",
        description = "활성 약관별로 내 동의 상태(AGREED/NEEDS_RECONSENT/NOT_AGREED)와 동의 버전을 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @GetMapping("/me")
    fun getMyConsents(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<List<TermsConsentStatusResponse>>> {
        return Response.ok(termsService.getMyConsentStatus(principal.id!!)).toResponseEntity()
    }

    @Operation(
        summary = "약관 동의/철회",
        description = "각 항목의 코드가 가리키는 약관의 최신 버전에 동의(agreed=true) 또는 철회(agreed=false) 행을 append합니다. 필수 약관은 철회할 수 없습니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "처리 성공"),
        ApiResponse(responseCode = "400", description = "필수 약관 철회 시도"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않거나 비활성화된 약관 코드")
    )
    @PostMapping
    fun consent(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: List<@Valid ConsentItem>
    ): ResponseEntity<Response<Unit>> {
        termsService.consent(principal.id!!, request)
        return Response.ok().toResponseEntity()
    }
}
