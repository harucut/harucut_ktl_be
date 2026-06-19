package com.harucut.auth.security.handler

import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.oauth2.CustomOAuth2User
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class CustomOAuth2SuccessHandler(
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val cookieManager: CookieManager,
    @Value("\${redirect-url.frontend}") private val frontendUrl: String
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as CustomOAuth2User
        val publicId = principal.publicId

        val accessToken = jwtTokenService.createAccessToken(publicId)
        val refreshToken = jwtTokenService.createRefreshToken(publicId)

        refreshTokenService.saveRefreshToken(publicId, refreshToken)

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieManager.createTokenCookie(
                "accessToken", accessToken, jwtTokenService.getAccessTokenValidityMillis()
            ).toString()
        )
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieManager.createTokenCookie(
                "refreshToken", refreshToken, jwtTokenService.getRefreshTokenValidityMillis()
            ).toString()
        )

        redirectStrategy.sendRedirect(request, response, "$frontendUrl/oauth2/callback")
    }
}