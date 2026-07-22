package com.harucut.payment.webhook

interface PaymentWebhookVerifier {

    // 웹훅 원문 body와 서명 헤더로 위변조 여부를 검증한다.
    fun verify(rawBody: String, signature: String?): Boolean
}
