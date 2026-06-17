package com.harucut.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.local.service.CustomUserDetailsService
import com.harucut.auth.security.CustomAuthenticationEntryPoint
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.util.mail.service.EmailVerificationService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@WebMvcTest(AuthEmailController::class)
@Import(SecurityConfig::class)
class AuthEmailControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var customUserDetailsService: CustomUserDetailsService

    @MockkBean
    lateinit var jwtTokenService: JwtTokenService

    @MockkBean
    lateinit var customAuthenticationEntryPoint: CustomAuthenticationEntryPoint

    @MockkBean
    lateinit var emailVerificationService: EmailVerificationService

    @Nested
    @DisplayName("POST /api/email-auth/code")
    inner class SendVerificationCode {

        @Test
        @DisplayName("정상 요청 시 200을 반환한다")
        fun success() {
            // given
            every { emailVerificationService.sendVerificationCode(any()) } just Runs

            // when & then
            mockMvc.post("/api/email-auth/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to "test@harucut.com"))
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        fun invalidEmail() {
            // when & then
            mockMvc.post("/api/email-auth/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to "not-an-email"))
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("이메일이 빈 값이면 400을 반환한다")
        fun emptyEmail() {
            // when & then
            mockMvc.post("/api/email-auth/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to ""))
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("메일 발송 실패 시 500을 반환한다")
        fun mailSendFailed() {
            // given
            every { emailVerificationService.sendVerificationCode(any()) } throws
                    BusinessException(AuthErrorCode.EMAIL_SEND_FAILED)

            // when & then
            mockMvc.post("/api/email-auth/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to "test@harucut.com"))
            }.andExpect {
                status { isInternalServerError() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/email-auth/verification")
    inner class VerifyCode {

        @Test
        @DisplayName("올바른 코드 입력 시 200을 반환한다")
        fun success() {
            // given
            every { emailVerificationService.verifyCode(any(), any()) } returns true

            // when & then
            mockMvc.post("/api/email-auth/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "code" to "ABC123")
                )
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("코드가 틀리거나 만료되면 400을 반환한다")
        fun wrongOrExpiredCode() {
            // given
            every { emailVerificationService.verifyCode(any(), any()) } returns false

            // when & then
            mockMvc.post("/api/email-auth/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "code" to "WRONG1")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        fun invalidEmail() {
            // when & then
            mockMvc.post("/api/email-auth/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "not-an-email", "code" to "ABC123")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("코드가 빈 값이면 400을 반환한다")
        fun emptyCode() {
            // when & then
            mockMvc.post("/api/email-auth/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "code" to "")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("이메일이 빈 값이면 400을 반환한다")
        fun blankEmail() {
            mockMvc.post("/api/email-auth/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "", "code" to "ABC123")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }
}