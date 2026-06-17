package com.harucut.util.mail.service

import com.harucut.exception.BusinessException
import io.mockk.*
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import kotlin.test.Test

class MailServiceTest {

    private val mailSender: JavaMailSender = mockk()
    private val service = MailServiceImpl(mailSender)

    @Nested
    inner class SendEmail {

        @Test
        @DisplayName("메일 전송 정상 요청")
        fun success() {
            // given
            val mimeMessage = mockk<MimeMessage>(relaxed = true)
            every { mailSender.createMimeMessage() } returns mimeMessage
            every { mailSender.send(mimeMessage) } just Runs

            // when
            service.sendEmail("test@harucut.com", "제목", "내용", false)

            // then
            verify { mailSender.send(mimeMessage) }
        }

        @Test
        @DisplayName("메일 구성 중 MessagingException 발생 시 BusinessException을 던진다")
        fun messagingException() {
            // given
            val mimeMessage = mockk<MimeMessage>(relaxed = true)
            every { mailSender.createMimeMessage() } returns mimeMessage
            every { mimeMessage.setRecipient(any(), any()) } throws MessagingException("smtp error")

            // when & then
            assertThatThrownBy { service.sendEmail("test@harucut.com", "제목", "내용", false) }
                .isInstanceOf(BusinessException::class.java)
        }

        @Test
        @DisplayName("메일 발송 중 MailException 발생 시 BusinessException을 던진다")
        fun mailException() {
            // given
            val mimeMessage = mockk<MimeMessage>(relaxed = true)
            every { mailSender.createMimeMessage() } returns mimeMessage
            every { mailSender.send(mimeMessage) } throws MailSendException("send failed")

            // when & then
            assertThatThrownBy { service.sendEmail("test@harucut.com", "제목", "내용", false) }
                .isInstanceOf(BusinessException::class.java)
        }
    }

}