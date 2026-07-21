package com.harucut.payment.webhook

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

// 실 PG 연동 시 서명(HMAC 등) 검증 로직으로 교체한다(TODO). Mock은 항상 통과시킨다.
@Component
@ConditionalOnProperty(prefix = "payment.gateway", name = ["provider"], havingValue = "mock", matchIfMissing = true)
class MockPaymentWebhookVerifier : PaymentWebhookVerifier {

    override fun verify(rawBody: String, signature: String?): Boolean = true
}
