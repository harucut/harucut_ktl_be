package com.harucut.util.mail.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

@Service
class MailServiceImpl(
    private val mailSender: JavaMailSender,
) : MailService {

    override fun sendEmail(to: String, subject: String, text: String, isHtml: Boolean) {
        val mimeMessage: MimeMessage = mailSender.createMimeMessage()

        try {
            val helper = MimeMessageHelper(mimeMessage, isHtml, StandardCharsets.UTF_8.name())
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(text, isHtml)
            mailSender.send(mimeMessage)
        } catch (e: MessagingException) {
            throw BusinessException(AuthErrorCode.EMAIL_SEND_FAILED)
        } catch (e: MailException) {
            throw BusinessException(AuthErrorCode.EMAIL_SEND_FAILED)
        }
    }
}