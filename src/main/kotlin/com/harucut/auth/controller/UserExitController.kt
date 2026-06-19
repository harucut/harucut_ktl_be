package com.harucut.auth.controller

import com.harucut.auth.exit.service.UserExitService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth (Exit)", description = "회원 탈퇴 요청 및 탈퇴 취소 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/harucut")
class UserExitController(
    private val userExitService: UserExitService,
    private val cookieManager: CookieManager
) {

    @Operation(
        summary = "회원 탈퇴 요청",
        description = "탈퇴 요청(DELETED_REQUESTED) 후 토큰 쿠키를 만료시킵니다. 7일 뒤 자정에 하드삭제됩니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "탈퇴 요청 성공 (토큰 쿠키 만료)"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @DeleteMapping("/exit")
    fun exit(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        userExitService.requestExit(principal.id!!)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieManager.createExpiredCookie("accessToken").toString())
            .header(HttpHeaders.SET_COOKIE, cookieManager.createExpiredCookie("refreshToken").toString())
            .body(Response.ok())
    }

    @Operation(
        summary = "회원 탈퇴 취소",
        description = "유예기간(DELETED_REQUESTED) 중인 사용자의 탈퇴를 취소하고 계정을 복구합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "탈퇴 취소 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "탈퇴 요청 상태가 아님 (권한 없음)"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @PreAuthorize("hasRole('DELETED_REQUESTED')")
    @PostMapping("/reactivate")
    fun reactivate(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        userExitService.reActivate(principal.id!!)

        return Response.ok().toResponseEntity()
    }
}
