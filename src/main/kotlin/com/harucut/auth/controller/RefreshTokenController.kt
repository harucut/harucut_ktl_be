package com.harucut.auth.controller

import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/harucut")
class RefreshTokenController(
    private val refreshTokenService: RefreshTokenService,
    private val jwtTokenService: JwtTokenService,
    private val cookieManager: CookieManager
) {

    @PostMapping("/reissue")
    fun reissue(@CookieValue("refreshToken") refreshToken: String): ResponseEntity<Response<Unit>> {
        val tokens = refreshTokenService.reissue(refreshToken)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, tokens.accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, tokens.refreshTokenCookie.toString())
            .body(Response.ok())
    }

    @DeleteMapping("/logout")
    fun logout(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
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