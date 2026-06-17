package com.harucut.auth.local.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.repository.UserRepository
import com.harucut.util.mail.repository.EmailVerificationRepository
import com.harucut.util.mail.service.EmailVerificationService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration
import java.util.Optional
import kotlin.jvm.java

class PasswordServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val emailVerificationRepository: EmailVerificationRepository = mockk()
    private val emailVerificationService: EmailVerificationService = mockk()
    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val service = PasswordServiceImpl(
        userRepository, passwordEncoder, emailVerificationRepository, emailVerificationService, redisTemplate
    )

    init {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Nested
    @DisplayName("sendResetCode")
    inner class SendResetCode {

        @Test
        @DisplayName("존재하는 HARUCUT 유저면 비밀번호 재설정 코드를 발송한다")
        fun success() {
            // given
            val mockUser = mockk<User>()
            every { userRepository.findByProviderAndEmail(Provider.HARUCUT, "test@harucut.com") } returns mockUser
            every { emailVerificationService.sendPasswordResetCode("test@harucut.com") } just runs

            // when
            service.sendResetCode("test@harucut.com")

            // then
            verify { emailVerificationService.sendPasswordResetCode("test@harucut.com") }
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { userRepository.findByProviderAndEmail(Provider.HARUCUT, any()) } returns null

            // when & then
            assertThatThrownBy { service.sendResetCode("notfound@harucut.com") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }

    @Nested
    inner class VerifyAuthCode {

        @Test
        @DisplayName("코드 검증 성공 시 리셋 토큰을 반환한다")
        fun success() {
            // given
            every { emailVerificationRepository.verifyAndRemoveResetCode("test@harucut.com", "123456") } returns true
            every { valueOps.set(any<String>(), any<String>(), any<Duration>()) } just runs

            // when
            val result = service.verifyAuthCode("test@harucut.com", "123456")

            // then
            assertThat(result.resetToken).isNotBlank()
            verify { valueOps.set(match { it.startsWith("reset_token:") }, "test@harucut.com", any<Duration>()) }
        }

        @Test
        @DisplayName("코드가 일치하지 않으면 EMAIL_VERIFICATION_FAILED 예외를 던진다")
        fun codeMismatch() {
            // given
            every { emailVerificationRepository.verifyAndRemoveResetCode(any(), any()) } returns false

            // when & then
            assertThatThrownBy { service.verifyAuthCode("test@harucut.com", "wrong") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EMAIL_VERIFICATION_FAILED)
        }
    }

    @Nested
    inner class ResetPassword {

        private val resetToken = "valid-reset-token"
        private val email = "test@harucut.com"
        private val key = "reset_token:$resetToken"

        @Test
        @DisplayName("유효한 토큰으로 비밀번호를 변경한다")
        fun success() {
            // given
            val mockUser = mockk<User>()
            every { valueOps.get(key) } returns email
            every { userRepository.findByProviderAndEmail(Provider.HARUCUT, email) } returns mockUser
            every { passwordEncoder.encode("newPass1!") } returns "encodedNew"
            every { mockUser.changePassword("encodedNew") } just runs
            every { redisTemplate.delete(key) } returns true

            // when
            service.resetPassword(resetToken, "newPass1!")

            // then
            verify { mockUser.changePassword("encodedNew") }
            verify { redisTemplate.delete(key) }
        }

        @Test
        @DisplayName("토큰이 Redis에 없으면 INVALID_TOKEN 예외를 던진다")
        fun tokenNotFound() {
            // given
            every { valueOps.get(key) } returns null

            // when & then
            assertThatThrownBy { service.resetPassword(resetToken, "newPass1!") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }

        @Test
        @DisplayName("이메일에 해당하는 유저가 없으면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { valueOps.get(key) } returns email
            every { userRepository.findByProviderAndEmail(Provider.HARUCUT, email) } returns null

            // when & then
            assertThatThrownBy { service.resetPassword(resetToken, "newPass1!") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }

    @Nested
    inner class ChangePassword {

        private val userId = 1L
        private val encodedOld = "encodedOld"

        @Test
        @DisplayName("기존 비밀번호가 일치하면 새 비밀번호로 변경한다")
        fun success() {
            // given
            val mockUser = mockk<User>()
            every { passwordEncoder.matches("oldPass1!", encodedOld) } returns true
            every { userRepository.findById(userId) } returns Optional.of(mockUser)
            every { passwordEncoder.encode("newPass1!") } returns "encodedNew"
            every { mockUser.changePassword("encodedNew") } just runs

            // when
            service.changePassword(userId, encodedOld, "oldPass1!", "newPass1!")

            // then
            verify { mockUser.changePassword("encodedNew") }
        }

        @Test
        @DisplayName("기존 비밀번호가 틀리면 WRONG_PASSWORD 예외를 던진다")
        fun wrongPassword() {
            // given
            every { passwordEncoder.matches("wrongPass!", encodedOld) } returns false

            // when & then
            assertThatThrownBy { service.changePassword(userId, encodedOld, "wrongPass!", "newPass1!") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.WRONG_PASSWORD)
        }

        @Test
        @DisplayName("소셜 로그인 유저(비밀번호 없음)는 WRONG_PASSWORD 예외를 던진다")
        fun socialUserNoPassword() {
            // given
            every { passwordEncoder.matches("anyPass!", null) } returns false

            // when & then
            assertThatThrownBy { service.changePassword(userId, null, "anyPass!", "newPass1!") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.WRONG_PASSWORD)
        }

        @Test
        @DisplayName("유저가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { passwordEncoder.matches("oldPass1!", encodedOld) } returns true
            every { userRepository.findById(userId) } returns Optional.empty()

            // when & then
            assertThatThrownBy { service.changePassword(userId, encodedOld, "oldPass1!", "newPass1!") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }
}