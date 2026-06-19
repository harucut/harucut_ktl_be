package com.harucut.auth.security.filter

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.dto.JwtClaims
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.auth.security.service.CustomUserDetailsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint

class JwtAuthenticationFilterTest {

    private val jwtTokenService: JwtTokenService = mockk()
    private val userDetailsService: CustomUserDetailsService = mockk()
    private val authenticationEntryPoint: AuthenticationEntryPoint = mockk(relaxed = true)

    private val filter = JwtAuthenticationFilter(jwtTokenService, userDetailsService, authenticationEntryPoint)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun principal(): CustomUserPrincipal = mockk {
        every { authorities } returns listOf(SimpleGrantedAuthority("ROLE_USER"))
    }

    @Nested
    @DisplayName("토큰 해석")
    inner class ResolveToken {

        @Test
        @DisplayName("accessToken 쿠키가 있으면 인증 컨텍스트를 설정한다")
        fun fromCookie() {
            // given
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie("accessToken", "valid-token"))
            }
            val chain = MockFilterChain()
            every { jwtTokenService.parse("valid-token") } returns JwtClaims("pub-1", "ACCESS")
            every { userDetailsService.loadUserByPublicId("pub-1") } returns principal()

            // when
            filter.doFilter(request, MockHttpServletResponse(), chain)

            // then
            assertThat(SecurityContextHolder.getContext().authentication).isNotNull()
            assertThat(chain.request).isNotNull()
        }

        @Test
        @DisplayName("쿠키가 없고 Authorization: Bearer 헤더가 있으면 동일하게 인증한다")
        fun fromBearerHeader() {
            // given
            val request = MockHttpServletRequest().apply {
                addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token")
            }
            val chain = MockFilterChain()
            every { jwtTokenService.parse("header-token") } returns JwtClaims("pub-1", "ACCESS")
            every { userDetailsService.loadUserByPublicId("pub-1") } returns principal()

            // when
            filter.doFilter(request, MockHttpServletResponse(), chain)

            // then
            assertThat(SecurityContextHolder.getContext().authentication).isNotNull()
            assertThat(chain.request).isNotNull()
        }

        @Test
        @DisplayName("쿠키가 우선한다 - 쿠키와 헤더가 모두 있으면 쿠키 토큰으로 파싱한다")
        fun cookieTakesPrecedence() {
            // given
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie("accessToken", "cookie-token"))
                addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token")
            }
            every { jwtTokenService.parse("cookie-token") } returns JwtClaims("pub-1", "ACCESS")
            every { userDetailsService.loadUserByPublicId("pub-1") } returns principal()

            // when
            filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

            // then
            verify { jwtTokenService.parse("cookie-token") }
            verify(exactly = 0) { jwtTokenService.parse("header-token") }
        }

        @Test
        @DisplayName("토큰이 전혀 없으면 인증 없이 필터 체인을 통과시킨다")
        fun noToken() {
            // given
            val request = MockHttpServletRequest()
            val chain = MockFilterChain()

            // when
            filter.doFilter(request, MockHttpServletResponse(), chain)

            // then
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
            assertThat(chain.request).isNotNull()
            verify(exactly = 0) { jwtTokenService.parse(any()) }
        }

        @Test
        @DisplayName("Bearer 접두사가 아닌 Authorization 헤더는 무시한다")
        fun nonBearerHeaderIgnored() {
            // given
            val request = MockHttpServletRequest().apply {
                addHeader(HttpHeaders.AUTHORIZATION, "Basic abc123")
            }

            // when
            filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

            // then
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
            verify(exactly = 0) { jwtTokenService.parse(any()) }
        }
    }

    @Nested
    @DisplayName("토큰 검증 실패")
    inner class InvalidToken {

        @Test
        @DisplayName("토큰 파싱이 실패하면 컨텍스트를 비우고 EntryPoint를 호출하며 체인을 중단한다")
        fun parseFailure() {
            // given
            val request = MockHttpServletRequest().apply {
                addHeader(HttpHeaders.AUTHORIZATION, "Bearer broken-token")
            }
            val chain = MockFilterChain()
            every { jwtTokenService.parse("broken-token") } throws
                    CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN)

            // when
            filter.doFilter(request, MockHttpServletResponse(), chain)

            // then
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
            assertThat(chain.request).isNull()
            verify { authenticationEntryPoint.commence(any(), any(), any()) }
        }
    }
}
