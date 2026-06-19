package com.harucut.auth.controller

import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth (Token)", description = "JWT 토큰 재발급 및 로그아웃 API")
@RestController
@RequestMapping("/api/harucut")
class RefreshTokenController(
    private val refreshTokenService: RefreshTokenService,
    private val jwtTokenService: JwtTokenService,
    private val cookieManager: CookieManager
) {

    @Operation(
        summary = "토큰 재발급",
        description = "쿠키의 Refresh Token을 검증하여 새 Access/Refresh 토큰을 쿠키로 재발급합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "재발급 성공 (Set-Cookie)"),
        ApiResponse(responseCode = "400", description = "refreshToken 쿠키 누락"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    )
    @PostMapping("/reissue")
    fun reissue(
        @Parameter(description = "리프레시 토큰 쿠키", required = true)
        @CookieValue("refreshToken") refreshToken: String
    ): ResponseEntity<Response<Unit>> {
        val tokens = refreshTokenService.reissue(refreshToken)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, tokens.accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, tokens.refreshTokenCookie.toString())
            .body(Response.ok())
    }

    @Operation(
        summary = "로그아웃",
        description = "서버(Redis)의 Refresh Token을 삭제하고 토큰 쿠키를 만료시킵니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그아웃 성공 (토큰 쿠키 만료)")
    )
    @DeleteMapping("/logout")
    fun logout(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @Parameter(description = "리프레시 토큰 쿠키", required = false)
        @CookieValue(value = "refreshToken", required = false) refreshToken: String?
    ): ResponseEntity<Response<Unit>> {
        resolvePublicId(principal, refreshToken)?.let {
            refreshTokenService.logout(it)
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieManager.createExpiredCookie("accessToken").toString())
            .header(HttpHeaders.SET_COOKIE, cookieManager.createExpiredCookie("refreshToken").toString())
            .body(Response.ok())
    }

    private fun resolvePublicId(principal: CustomUserPrincipal?, refreshToken: String?): String? {
        if (principal != null) return principal.publicId
        if (refreshToken.isNullOrBlank()) return null

        return try {
            val claims = jwtTokenService.parse(refreshToken)
            if (claims.tokenType != "REFRESH") null else claims.publicId
        } catch (e: Exception) {
            null
        }
    }
}
