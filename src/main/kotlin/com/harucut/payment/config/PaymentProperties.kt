package com.harucut.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties

// 결제 설정 — payment.* (기본값: mock 게이트웨이, KRW, 유예 3일)
@ConfigurationProperties(prefix = "payment")
data class PaymentProperties(
    val gateway: Gateway = Gateway(),
    val currency: String = "KRW",
    val webhookSecret: String = "",
    val mock: Mock = Mock(),
    val graceDays: Long = 3
) {
    data class Gateway(val provider: String = "mock")
    data class Mock(var failCharge: Boolean = false)
}
