package com.harucut.auth.controller

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.jwt.dto.JwtClaims
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.local.service.CustomUserDetailsService
import com.harucut.auth.security.CustomAuthenticationEntryPoint
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseCookie
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post

@WebMvcTest(RefreshTokenController::class)
@Import(SecurityConfig::class)
class RefreshTokenControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockkBean
    lateinit var jwtTokenService: JwtTokenService

    @MockkBean
    lateinit var cookieManager: CookieManager

    @MockkBean
    lateinit var customUserDetailsService: CustomUserDetailsService

    @MockkBean
    lateinit var customAuthenticationEntryPoint: CustomAuthenticationEntryPoint

    private fun mockCookie(name: String) =
        ResponseCookie.from(name, "value").build()

    @Nested
    @DisplayName("POST /api/harucut/reissue")
    inner class Reissue {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 토큰 쿠키를 발급하고 200을 반환한다")
        fun success() {
            // given
            every { refreshTokenService.reissue(any()) } returns AuthTokenCookies(
                mockCookie("accessToken"),
                mockCookie("refreshToken")
            )

            // when & then
            mockMvc.post("/api/harucut/reissue") {
                cookie(Cookie("refreshToken", "valid-refresh-token"))
            }.andExpect {
                status { isOk() }
                header { exists("Set-Cookie") }
            }
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰이면 401을 반환한다")
        fun invalidToken() {
            // given
            every { refreshTokenService.reissue(any()) } throws
                    BusinessException(AuthErrorCode.INVALID_TOKEN)

            // when & then
            mockMvc.post("/api/harucut/reissue") {
                cookie(Cookie("refreshToken", "invalid-token"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/harucut/logout")
    inner class Logout {

        @Test
        @DisplayName("로그아웃 시 쿠키를 만료시키고 200을 반환한다")
        fun success() {
            // given
            every { refreshTokenService.logout(any()) } just Runs
            every { jwtTokenService.parse(any()) } returns JwtClaims("abc123", "REFRESH")
            every { cookieManager.createExpiredCookie(any()) } returns mockCookie("expired")

            // when & then
            mockMvc.delete("/api/harucut/logout") {
                cookie(Cookie("refreshToken", "refresh-token"))
            }.andExpect {
                status { isOk() }
                header { exists("Set-Cookie") }
            }
        }

        @Test
        @DisplayName("쿠키 없이 로그아웃해도 200을 반환한다")
        fun withoutCookies() {
            // given
            every { cookieManager.createExpiredCookie(any()) } returns mockCookie("expired")

            // when & then
            mockMvc.delete("/api/harucut/logout").andExpect {
                status { isOk() }
            }
        }
    }
}