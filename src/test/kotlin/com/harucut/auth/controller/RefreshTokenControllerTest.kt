package com.harucut.auth.controller

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.jwt.dto.JwtClaims
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.local.service.CustomUserDetailsService
import com.harucut.auth.security.CustomAuthenticationEntryPoint
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

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
        @DisplayName("refreshToken 쿠키가 없으면 400을 반환한다")
        fun noCookie() {
            mockMvc.post("/api/harucut/reissue").andExpect {
                status { isBadRequest() }
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

        @Test
        @DisplayName("인증된 사용자로 로그아웃 시 publicId로 Redis 토큰을 삭제한다")
        fun withPrincipal() {
            // given
            val user = User(
                provider = Provider.HARUCUT,
                userRole = UserRole.ROLE_USER,
                email = "test@harucut.com",
                username = "tester",
                profileImageUrl = "default.png",
                userStatus = UserStatus.ACTIVE
            )
            val principal = CustomUserPrincipal(user)
            val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

            every { refreshTokenService.logout(principal.publicId) } just Runs
            every { cookieManager.createExpiredCookie(any()) } returns mockCookie("expired")

            // when & then
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/harucut/logout")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
            ).andExpect(MockMvcResultMatchers.status().isOk())

            verify { refreshTokenService.logout(principal.publicId) }
        }

        @Test
        @DisplayName("refreshToken 타입이 REFRESH가 아니면 logout을 호출하지 않고 200을 반환한다")
        fun invalidTokenType() {
            // given
            every { jwtTokenService.parse(any()) } returns JwtClaims("abc123", "ACCESS")
            every { cookieManager.createExpiredCookie(any()) } returns mockCookie("expired")

            // when & then
            mockMvc.delete("/api/harucut/logout") {
                cookie(Cookie("refreshToken", "access-token"))
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 0) { refreshTokenService.logout(any()) }
        }

        @Test
        @DisplayName("refreshToken 파싱 중 예외가 발생하면 logout을 호출하지 않고 200을 반환한다")
        fun parseException() {
            // given
            every { jwtTokenService.parse(any()) } throws RuntimeException("parse error")
            every { cookieManager.createExpiredCookie(any()) } returns mockCookie("expired")

            // when & then
            mockMvc.delete("/api/harucut/logout") {
                cookie(Cookie("refreshToken", "broken-token"))
            }.andExpect {
                status { isOk() }
            }

            verify (exactly = 0) { refreshTokenService.logout(any()) }
        }
    }
}