package com.harucut.auth.security.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class CustomOAuth2FailureHandler(
    @Value("\${redirect-url.frontend}") private val frontendUrl: String
) : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        logger.warn("OAuth2 인증 실패: ${exception.message}", exception)
        redirectStrategy.sendRedirect(request, response, "$frontendUrl/oauth2/callback?error=oauth2")
    }
}