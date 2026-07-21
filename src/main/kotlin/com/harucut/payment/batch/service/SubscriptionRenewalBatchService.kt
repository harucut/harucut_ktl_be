package com.harucut.payment.batch.service

import com.harucut.payment.gateway.PaymentGateway
import com.harucut.payment.gateway.dto.BillingChargeCommand
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

// 구독 1건의 정기결제 재청구. 스케줄러가 항목별로 호출하며, 실패해도 예외를 던지지 않고
// PAST_DUE로 반영해 커밋하므로 다른 구독 처리에 영향을 주지 않는다.
// 최초결제(PaymentServiceImpl)와 동일하게 "주문 선커밋 → PG 청구 → 결과 반영"을 REQUIRES_NEW로
// 분리한 SubscriptionRenewalTransactionService에 위임한다 — PG 성공 후 커밋 실패로 멱등키 주문이
// 롤백되어 다음날 이중청구되는 것을 막기 위함.
@Service
class SubscriptionRenewalBatchService(
    private val transactionService: SubscriptionRenewalTransactionService,
    private val paymentGateway: PaymentGateway,
    private val clock: Clock
) {

    fun renewInNewTransaction(subscriptionId: Long) {
        val now = LocalDateTime.now(clock)
        val preparation = transactionService.prepareRenewalOrderInNewTransaction(subscriptionId, now)
        val created = preparation as? SubscriptionRenewalTransactionService.RenewalPreparation.Created ?: return

        val chargeResult = paymentGateway.charge(
            BillingChargeCommand(
                billingKeyValue = created.billingKeyValue,
                orderKey = created.orderPublicId,
                amount = created.amount,
                orderName = "${created.planTier.name} 구독 갱신",
                customerKey = created.userPublicId
            )
        )

        transactionService.applyRenewalChargeResultInNewTransaction(
            subscriptionId, created.orderId, created.amount, chargeResult, now
        )
    }
}
