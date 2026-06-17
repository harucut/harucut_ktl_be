package com.harucut.util.mail.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.util.mail.component.VerificationCodeGenerator
import com.harucut.util.mail.repository.EmailVerificationRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.thymeleaf.spring6.SpringTemplateEngine
import kotlin.test.Test

class EmailVerificationServiceTest {

    private val generator: VerificationCodeGenerator = mockk()
    private val repository: EmailVerificationRepository = mockk()
    private val mailService: MailService = mockk()
    private val templateEngine: SpringTemplateEngine = mockk()

    private val service = EmailVerificationServiceImpl(generator, repository, mailService, templateEngine)

    @Nested
    inner class SendVerificationCode {

        @Test
        @DisplayName("코드 생성 → 저장 → 렌더링 → 발송 순서로 실행된다")
        fun success() {
            // given
            val email = "test@harucut.com"
            val code = "ABC123"

            every { generator.generate() } returns code
            every { repository.saveCode(email, code) } just Runs
            every { templateEngine.process(any<String>(), any()) } returns "<html>$code</html>"
            every { mailService.sendEmail(any(), any(), any(), any()) } just Runs

            // when
            service.sendVerificationCode(email)

            // then
            verifyOrder {
                generator.generate()
                repository.saveCode(email, code)
                templateEngine.process(any<String>(), any())
                mailService.sendEmail(email, any(), any(), true)
            }
        }

        @Test
        @DisplayName("메일 발송 실패 시 예외가 전파된다")
        fun mailSendFailed() {
            // given
            every { generator.generate() } returns "ABC123"
            every { repository.saveCode(any(), any()) } just Runs
            every { templateEngine.process(any<String>(), any()) } returns "<html/>"
            every { mailService.sendEmail(any(), any(), any(), any()) } throws
                    BusinessException(AuthErrorCode.EMAIL_SEND_FAILED)

            // then
            assertThatThrownBy { service.sendVerificationCode("test@harucut.com") }
                .isInstanceOf(BusinessException::class.java)
        }
    }

    @Nested
    inner class VerifyCode {

        @Test
        @DisplayName("올바른 코드 입력 시 true를 반환하고 코드 삭제 후 인증 완료를 마킹한다")
        fun correctCode() {
            // given
            val email = "test@harucut.com"
            val code = "ABC123"

            every { repository.getCode(email) } returns code
            every { repository.removeCode(email) } just Runs
            every { repository.markVerified(email) } just Runs

            // when
            val result = service.verifyCode(email, code)

            // then
            assertThat(result).isTrue()
            verifyOrder {
                repository.removeCode(email)
                repository.markVerified(email)
            }
        }

        @Test
        @DisplayName("코드가 만료되었거나 없으면 false를 반환한다")
        fun expiredCode() {
            // given
            every { repository.getCode(any()) } returns null

            // when
            val result = service.verifyCode("test@harucut.com", "ABC123")

            // then
            assertThat(result).isFalse()
            verify(exactly = 0) { repository.removeCode(any()) }
            verify(exactly = 0) { repository.markVerified(any()) }
        }

        @Test
        @DisplayName("코드가 일치하지 않으면 false를 반환한다")
        fun wrongCode() {
            // given
            every { repository.getCode(any()) } returns "ABC123"

            // when
            val result = service.verifyCode("test@harucut.com", "WRONG1")

            // then
            assertThat(result).isFalse()
            verify(exactly = 0) { repository.removeCode(any()) }
            verify(exactly = 0) { repository.markVerified(any()) }
        }

        @Test
        @DisplayName("대소문자를 구분하지 않고 검증한다")
        fun caseInsensitive() {
            // given
            val email = "test@harucut.com"
            every { repository.getCode(email) } returns "abc123"
            every { repository.removeCode(email) } just Runs
            every { repository.markVerified(email) } just Runs

            // when
            val result = service.verifyCode(email, "ABC123")

            // then
            assertThat(result).isTrue()
        }
    }

    @Nested
    inner class ConsumeVerified {

        @Test
        @DisplayName("인증 완료된 이메일이면 true를 반환한다")
        fun verified() {
            // given
            every { repository.consumeVerified(any()) } returns true

            // when
            val result = service.consumeVerified("test@harucut.com")

            // then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("인증하지 않은 이메일이면 false를 반환한다")
        fun notVerified() {
            // given
            every { repository.consumeVerified(any()) } returns false

            // when
            val result = service.consumeVerified("test@harucut.com")

            // then
            assertThat(result).isFalse()
        }
    }
}