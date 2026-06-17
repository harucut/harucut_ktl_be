package com.harucut.auth.local.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.dto.LoginResult
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.local.service.CustomUserDetailsService
import com.harucut.auth.local.service.LocalLoginService
import com.harucut.auth.local.service.LocalRegisterService
import com.harucut.auth.security.CustomAuthenticationEntryPoint
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.user.enums.UserStatus
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(LocalAuthController::class)
@Import(SecurityConfig::class)
class LocalAuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var localRegisterService: LocalRegisterService

    @MockkBean
    lateinit var localLoginService: LocalLoginService

    @MockkBean
    lateinit var customUserDetailsService: CustomUserDetailsService

    @MockkBean
    lateinit var jwtTokenService: JwtTokenService

    @MockkBean
    lateinit var customAuthenticationEntryPoint: CustomAuthenticationEntryPoint

    @Nested
    @DisplayName("POST /api/harucut/register")
    inner class Register {

        @Test
        @DisplayName("정상 요청 시 200을 반환한다")
        fun success() {
            // given
            every { localRegisterService.register(any()) } just Runs

            // when & then
            mockMvc.post("/api/harucut/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "username" to "tester", "password" to "password1!")
                )
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        fun invalidEmail() {
            // when & then
            mockMvc.post("/api/harucut/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "not-email", "username" to "tester", "password" to "password1!")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400을 반환한다")
        fun shortPassword() {
            // when & then
            mockMvc.post("/api/harucut/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "username" to "tester", "password" to "short")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 409를 반환한다")
        fun duplicateEmail() {
            // given
            every { localRegisterService.register(any()) } throws
                    BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS)

            // when & then
            mockMvc.post("/api/harucut/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "username" to "tester", "password" to "password1!")
                )
            }.andExpect {
                status { isConflict() }
            }
        }

        @Test
        @DisplayName("이메일 인증이 안 됐으면 400을 반환한다")
        fun notVerified() {
            // given
            every { localRegisterService.register(any()) } throws
                    BusinessException(AuthErrorCode.EMAIL_REGISTRATION_FAILED)

            // when & then
            mockMvc.post("/api/harucut/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "username" to "tester", "password" to "password1!")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("username이 빈 값이면 400을 반환한다")
        fun blankUsername() {
            mockMvc.post("/api/harucut/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "username" to "", "password" to "password1!")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/harucut/login")
    inner class Login {

        private val cookie = ResponseCookie.from("token", "value").build()

        @Test
        @DisplayName("정상 로그인 시 200과 Set-Cookie 헤더를 반환한다")
        fun success() {
            // given
            every { localLoginService.login(any()) } returns LoginResult(
                cookies = AuthTokenCookies(cookie, cookie),
                userStatus = UserStatus.ACTIVE
            )

            // when & then
            mockMvc.post("/api/harucut/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "password" to "password1!")
                )
            }.andExpect {
                status { isOk() }
                header { exists("Set-Cookie") }
            }
        }

        @Test
        @DisplayName("비밀번호가 틀리면 400을 반환한다")
        fun wrongPassword() {
            // given
            every { localLoginService.login(any()) } throws
                    BusinessException(AuthErrorCode.INVALID_CREDENTIALS)

            // when & then
            mockMvc.post("/api/harucut/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "password" to "wrong")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("존재하지 않는 유저면 404를 반환한다")
        fun userNotFound() {
            // given
            every { localLoginService.login(any()) } throws
                    BusinessException(AuthErrorCode.USER_NOT_FOUND)

            // when & then
            mockMvc.post("/api/harucut/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "no@harucut.com", "password" to "password1!")
                )
            }.andExpect {
                status { isNotFound() }
            }
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        fun invalidEmail() {
            mockMvc.post("/api/harucut/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "not-email", "password" to "password1!")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("비밀번호가 빈 값이면 400을 반환한다")
        fun blankPassword() {
            mockMvc.post("/api/harucut/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "password" to "")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("일반 인증 실패 시 401을 반환한다")
        fun authenticationFailed() {
            // given
            every { localLoginService.login(any()) } throws
                    BusinessException(AuthErrorCode.AUTHENTICATION_FAILED)

            // when & then
            mockMvc.post("/api/harucut/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "password" to "password1!")
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}