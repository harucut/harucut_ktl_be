package com.harucut.auth.controller

import com.harucut.auth.dto.AuthStatusResponse
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth (Status)", description = "로그인 사용자 인증 상태 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth")
class AuthStatusController {

    // 현재 로그인 사용자의 상태 반환 (프론트 로그인 체크용)
    @Operation(summary = "인증 상태 조회", description = "현재 로그인된 사용자의 상태를 반환합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @GetMapping("/status")
    fun status(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<AuthStatusResponse>> =
        Response.ok(AuthStatusResponse(principal.status)).toResponseEntity()
}
