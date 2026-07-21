package com.harucut.payment.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// 웹훅 골격. 실 PG 연동 시 rawBody를 파싱해 pgTransactionId 기준으로
// Payment/PaymentOrder 상태를 멱등하게 동기화하는 로직으로 교체한다(TODO).
@Service
class WebhookServiceImpl : WebhookService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(rawBody: String) {
        // 민감정보(카드/거래 상세)가 섞일 수 있는 전문은 로그에 남기지 않고 길이만 기록한다.
        log.info("[결제 웹훅 수신] bodyLength={}", rawBody.length)
    }
}
