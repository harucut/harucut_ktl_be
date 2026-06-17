package com.harucut.auth.local.service

import com.harucut.auth.dto.LocalLoginRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.Authentication

class LocalLoginServiceTest {

    private val authenticationManager: AuthenticationManager = mockk()
    private val jwtTokenService: JwtTokenService = mockk(relaxed = true)
    private val refreshTokenService: RefreshTokenService = mockk(relaxed = true)
    private val cookieManager: CookieManager = mockk(relaxed = true)
    private val service = LocalLoginServiceImpl(
        authenticationManager, jwtTokenService, refreshTokenService, cookieManager
    )

    private val request = LocalLoginRequest("test@harucut.com", "password1!")

    private fun principal(): CustomUserPrincipal {
        val user = User(
            provider = Provider.HARUCUT,
            userRole = UserRole.ROLE_USER,
            email = "test@harucut.com",
            username = "tester",
            profileImageUrl = "default.png",
            userStatus = UserStatus.ACTIVE
        )
        return CustomUserPrincipal(user)
    }

    @Nested
    inner class Login {

        @Test
        @DisplayName("올바른 이메일/비밀번호로 LoginResult를 반환한다")
        fun success() {
            // given
            val principal = principal()
            val authentication = mockk<Authentication>()

            every { authenticationManager.authenticate(any()) } returns authentication
            every { authentication.principal } returns principal

            // when
            val result = service.login(request)

            // then
            assertThat(result.userStatus).isEqualTo(UserStatus.ACTIVE)
            assertThat(result.cookies.accessTokenCookie).isNotNull
            verify { refreshTokenService.saveRefreshToken(principal.publicId, any()) }
        }

        @Test
        @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS 예외를 던진다")
        fun wrongPassword() {
            // given
            every { authenticationManager.authenticate(any()) } throws BadCredentialsException("bad credentials")

            // when & then
            assertThatThrownBy { service.login(request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
        }

        @Test
        @DisplayName("존재하지 않는 유저면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            val cause = CustomAuthenticationException(AuthErrorCode.USER_NOT_FOUND)
            every { authenticationManager.authenticate(any()) } throws
                    InternalAuthenticationServiceException("user not found", cause)

            // when & then
            assertThatThrownBy { service.login(request) }
                .isInstanceOf(BusinessException::class.java)
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }

        @Test
        @DisplayName("cause가 CustomAuthenticationException이 아닌 인증 실패는 AUTHENTICATION_FAILED 예외를 던진다")
        fun authenticationFailed() {
            // given
            every { authenticationManager.authenticate(any()) } throws
                    InternalAuthenticationServiceException("internal error", RuntimeException("not custom"))

            // when & then
            assertThatThrownBy { service.login(request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED)
        }
    }
}