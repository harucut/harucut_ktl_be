package com.harucut.auth.local.service

import com.harucut.auth.dto.LocalRegisterRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.repository.UserRepository
import com.harucut.util.mail.service.EmailVerificationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class LocalRegisterServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk(relaxed = true)
    private val emailVerificationService: EmailVerificationService = mockk()
    private val service = LocalRegisterServiceImpl(userRepository, passwordEncoder, emailVerificationService)

    private val request = LocalRegisterRequest(
        email = "test@harucut.com",
        username = "tester",
        password = "password1!"
    )

    @Nested
    inner class Register {

        @Test
        @DisplayName("정상 요청 시 유저를 저장한다")
        fun success() {
            // given
            every { userRepository.existsByProviderAndEmail(Provider.HARUCUT, request.email) } returns false
            every { emailVerificationService.consumeVerified(request.email) } returns true
            every { userRepository.save(any<User>()) } returnsArgument 0

            // when
            service.register(request)

            // then
            verify { userRepository.save(any()) }
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 EMAIL_ALREADY_EXISTS 예외를 던진다")
        fun duplicateEmail() {
            // given
            every { userRepository.existsByProviderAndEmail(Provider.HARUCUT, request.email) } returns true

            // when & then
            assertThatThrownBy { service.register(request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        @Test
        @DisplayName("이메일 인증이 안 된 경우 EMAIL_REGISTRATION_FAILED 예외를 던진다")
        fun notVerified() {
            // given
            every { userRepository.existsByProviderAndEmail(Provider.HARUCUT, request.email) } returns false
            every { emailVerificationService.consumeVerified(request.email) } returns false

            // when & then
            assertThatThrownBy { service.register(request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EMAIL_REGISTRATION_FAILED)
        }
    }
}