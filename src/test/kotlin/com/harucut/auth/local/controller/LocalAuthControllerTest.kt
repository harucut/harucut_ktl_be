package com.harucut.auth.local.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.dto.LoginResult
import com.harucut.auth.dto.PasswordResetTokenResponse
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.local.service.LocalLoginService
import com.harucut.auth.local.service.LocalRegisterService
import com.harucut.auth.local.service.PasswordService
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@WebMvcTest(LocalAuthController::class)
@Import(SecurityConfig::class)
class LocalAuthControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var localRegisterService: LocalRegisterService

    @MockkBean
    lateinit var localLoginService: LocalLoginService

    @MockkBean
    lateinit var passwordService: PasswordService

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

    @Nested
    @DisplayName("POST /api/harucut/reset/password/code")
    inner class SendResetCode {

        @Test
        @DisplayName("존재하는 이메일이면 200을 반환한다")
        fun success() {
            // given
            every { passwordService.sendResetCode("test@harucut.com") } just Runs

            // when & then
            mockMvc.post("/api/harucut/reset/password/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com")
                )
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 404를 반환한다")
        fun userNotFound() {
            // given
            every { passwordService.sendResetCode(any()) } throws
                    BusinessException(AuthErrorCode.USER_NOT_FOUND)

            // when & then
            mockMvc.post("/api/harucut/reset/password/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "notfound@harucut.com")
                )
            }.andExpect {
                status { isNotFound() }
            }
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        fun invalidEmail() {
            mockMvc.post("/api/harucut/reset/password/code") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "not-email")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/harucut/reset/password/verification")
    inner class VerifyAuthCode {

        @Test
        @DisplayName("코드 검증 성공 시 200과 리셋 토큰을 반환한다")
        fun success() {
            // given
            every { passwordService.verifyAuthCode("test@harucut.com", "123456") } returns
                    PasswordResetTokenResponse("reset-token-uuid")

            // when & then
            mockMvc.post("/api/harucut/reset/password/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "code" to "123456")
                )
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.resetToken") { value("reset-token-uuid") }
            }
        }

        @Test
        @DisplayName("코드가 일치하지 않으면 400을 반환한다")
        fun codeMismatch() {
            // given
            every { passwordService.verifyAuthCode(any(), any()) } throws
                    BusinessException(AuthErrorCode.EMAIL_VERIFICATION_FAILED)

            // when & then
            mockMvc.post("/api/harucut/reset/password/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "code" to "wrong")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        fun invalidEmail() {
            mockMvc.post("/api/harucut/reset/password/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "not-email", "code" to "123456")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("코드가 빈 값이면 400을 반환한다")
        fun blankCode() {
            mockMvc.post("/api/harucut/reset/password/verification") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("email" to "test@harucut.com", "code" to "")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/harucut/reset/password")
    inner class ResetPassword {

        @Test
        @DisplayName("정상 요청 시 200을 반환한다")
        fun success() {
            // given
            every { passwordService.resetPassword(any(), any()) } just Runs

            // when & then
            mockMvc.patch("/api/harucut/reset/password") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("resetToken" to "valid-token", "newPassword" to "newPass1!")
                )
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 401을 반환한다")
        fun invalidToken() {
            // given
            every { passwordService.resetPassword(any(), any()) } throws
                    BusinessException(AuthErrorCode.INVALID_TOKEN)

            // when & then
            mockMvc.patch("/api/harucut/reset/password") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("resetToken" to "expired-token", "newPassword" to "newPass1!")
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면 400을 반환한다")
        fun shortPassword() {
            mockMvc.patch("/api/harucut/reset/password") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("resetToken" to "valid-token", "newPassword" to "short")
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/harucut/change/password")
    inner class ChangePassword {

        @Test
        @DisplayName("정상 요청 시 200을 반환한다")
        fun success() {
            // given
            val principal = authenticatedPrincipal()
            every { passwordService.changePassword(any(), any(), any(), any()) } just Runs

            // when & then
            mockMvc.patch("/api/harucut/change/password") {
                with(user(principal))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("oldPassword" to "oldPass1!", "newPassword" to "newPass1!")
                )
            }.andExpect {
                status { isOk() }
            }
        }
    }

    @Test
    @DisplayName("기존 비밀번호가 틀리면 400을 반환한다")
    fun wrongPassword() {
        // given
        val principal = authenticatedPrincipal()
        every { passwordService.changePassword(any(), any(), any(), any()) } throws
                BusinessException(AuthErrorCode.WRONG_PASSWORD)

        // when & then
        mockMvc.patch("/api/harucut/change/password") {
            with(user(principal))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("oldPassword" to "wrongOld!", "newPassword" to "newPass1!")
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("인증되지 않은 요청이면 401을 반환한다")
    fun unauthenticated() {
        // given
        every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
            secondArg<jakarta.servlet.http.HttpServletResponse>().status = 401
        }

        // when & then
        mockMvc.patch("/api/harucut/change/password") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("oldPassword" to "oldPass1!", "newPassword" to "newPass1!")
            )
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    private fun authenticatedPrincipal(): CustomUserPrincipal {
        val mockUser = mockk<User>()
        every { mockUser.id } returns 1L
        every { mockUser.publicId } returns "testPublicId"
        every { mockUser.email } returns "test@harucut.com"
        every { mockUser.password } returns "encodedPw"
        every { mockUser.userRole } returns UserRole.ROLE_USER
        every { mockUser.userStatus } returns UserStatus.ACTIVE
        return CustomUserPrincipal(mockUser)
    }
}