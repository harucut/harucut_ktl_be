package com.harucut.util.mail.service

import com.harucut.util.mail.component.VerificationCodeGenerator
import com.harucut.util.mail.repository.EmailVerificationRepository
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.context.Context

@Service
class EmailVerificationServiceImpl(
    private val generator: VerificationCodeGenerator,
    private val repository: EmailVerificationRepository,
    private val mailService: MailService,
    private val templateEngine: SpringTemplateEngine
) : EmailVerificationService {

    companion object {
        private const val VERIFICATION_SUBJECT = "[Harucut] 이메일 인증 코드입니다."
        private const val VERIFICATION_TEMPLATE = "verification-code"
    }

    override fun sendVerificationCode(email: String) {
        val code = generator.generate()
        repository.saveCode(email, code)

        val context = Context().apply { setVariable("code", code) }
        val html = templateEngine.process(VERIFICATION_TEMPLATE, context)

        mailService.sendEmail(email, VERIFICATION_SUBJECT, html, isHtml = true)
    }

    override fun verifyCode(email: String, code: String): Boolean {
        val storeCode = repository.getCode(email) ?: return false
        if (!storeCode.equals(code, ignoreCase = true)) return false

        repository.removeCode(email)
        repository.markVerified(email)
        return true
    }

    override fun consumeVerified(email: String): Boolean {
        return repository.consumeVerified(email)
    }
}