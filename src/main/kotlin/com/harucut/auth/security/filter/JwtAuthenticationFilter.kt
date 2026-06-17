package com.harucut.auth.security.filter

import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.local.service.CustomUserDetailsService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
}