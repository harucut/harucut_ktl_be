package com.harucut.payment.controller

import com.harucut.config.SecurityConfig
import com.harucut.payment.service.WebhookService
import com.harucut.payment.webhook.PaymentWebhookVerifier
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(PaymentWebhookController::class)
@Import(SecurityConfig::class)
class PaymentWebhookControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var paymentWebhookVerifier: PaymentWebhookVerifier

    @MockkBean
    lateinit var webhookService: WebhookService

    @Nested
    @DisplayName("POST /api/payments/webhook")
    inner class HandleWebhook {

        @Test
        @DisplayName("인증 없이도(공개 경로) 서명 검증에 성공하면 200을 반환한다")
        fun success() {
            every { paymentWebhookVerifier.verify(any(), any()) } returns true
            every { webhookService.handle(any()) } just Runs

            mockMvc.post("/api/payments/webhook") {
                header("X-Signature", "valid-signature")
                content = "{\"type\":\"PAYMENT_APPROVED\"}"
            }.andExpect {
                status { isOk() }
            }

            verify { webhookService.handle(any()) }
        }

        @Test
        @DisplayName("서명 검증에 실패하면 PAY-008을 400으로 반환한다")
        fun invalidSignature() {
            every { paymentWebhookVerifier.verify(any(), any()) } returns false

            mockMvc.post("/api/payments/webhook") {
                header("X-Signature", "invalid-signature")
                content = "{\"type\":\"PAYMENT_APPROVED\"}"
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("PAY-008") }
            }

            verify(exactly = 0) { webhookService.handle(any()) }
        }
    }
}
