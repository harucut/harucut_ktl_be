package com.harucut.auth.security.filter

import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.security.service.CustomUserDetailsService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
    private val userDetailsService: CustomUserDetailsService,
    private val authenticationEntryPoint: AuthenticationEntryPoint
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessToken = resolveTokenFromCookie(request, "accessToken")
            ?: resolveTokenFromHeader(request)

        if (accessToken != null) {
            try {
                val claims = jwtTokenService.parse(accessToken)
                val principal = userDetailsService.loadUserByPublicId(claims.publicId)

                val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                    .apply { details = WebAuthenticationDetailsSource().buildDetails(request) }

                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: CustomAuthenticationException) {
                SecurityContextHolder.clearContext()
                authenticationEntryPoint.commence(request, response, e)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveTokenFromCookie(request: HttpServletRequest, name: String): String? =
        request.cookies
            ?.firstOrNull { it.name == name && StringUtils.hasText(it.value) }
            ?.value

    /**
     * 쿠키가 없을 때의 폴백: `Authorization: Bearer <token>` 헤더에서 토큰을 읽는다.
     * 운영 인증은 httpOnly 쿠키가 1차 경로이며, 헤더는 Swagger/API 클라이언트 테스트용 보조 경로.
     */
    private fun resolveTokenFromHeader(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        return if (header.startsWith(BEARER_PREFIX)) header.substring(BEARER_PREFIX.length) else null
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}