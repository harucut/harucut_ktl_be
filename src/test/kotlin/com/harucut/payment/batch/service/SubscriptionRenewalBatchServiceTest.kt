package com.harucut.payment.batch.service

import com.harucut.payment.gateway.PaymentGateway
import com.harucut.payment.gateway.dto.PaymentResult
import com.harucut.subscription.plan.PlanTier
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SubscriptionRenewalBatchServiceTest {

    private val transactionService = mockk<SubscriptionRenewalTransactionService>()
    private val paymentGateway = mockk<PaymentGateway>()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)

    private val service = SubscriptionRenewalBatchService(transactionService, paymentGateway, fixedClock)

    private fun created() = SubscriptionRenewalTransactionService.RenewalPreparation.Created(
        orderId = 10L,
        orderPublicId = "order-pub",
        billingKeyValue = "bk-value",
        userPublicId = "user-pub",
        planTier = PlanTier.PRO,
        amount = 9900
    )

    @Nested
    inner class RenewInNewTransaction {

        @Test
        @DisplayName("구독을 찾지 못하면(NotFound) 청구를 시도하지 않는다")
        fun notFound() {
            every {
                transactionService.prepareRenewalOrderInNewTransaction(any(), any())
            } returns SubscriptionRenewalTransactionService.RenewalPreparation.NotFound

            service.renewInNewTransaction(1L)

            verify(exactly = 0) { paymentGateway.charge(any()) }
            verify(exactly = 0) {
                transactionService.applyRenewalChargeResultInNewTransaction(any(), any(), any(), any(), any())
            }
        }

        @Test
        @DisplayName("빌링키가 없으면(준비 단계에서 이미 PAST_DUE 처리) 청구를 시도하지 않는다")
        fun noBillingKey() {
            every {
                transactionService.prepareRenewalOrderInNewTransaction(any(), any())
            } returns SubscriptionRenewalTransactionService.RenewalPreparation.NoBillingKey

            service.renewInNewTransaction(1L)

            verify(exactly = 0) { paymentGateway.charge(any()) }
        }

        @Test
        @DisplayName("같은 결제 주기에 이미 처리된 주문이 있으면(AlreadyProcessed) 재청구하지 않는다(멱등)")
        fun alreadyProcessed() {
            every {
                transactionService.prepareRenewalOrderInNewTransaction(any(), any())
            } returns SubscriptionRenewalTransactionService.RenewalPreparation.AlreadyProcessed

            service.renewInNewTransaction(1L)

            verify(exactly = 0) { paymentGateway.charge(any()) }
        }

        @Test
        @DisplayName("주문이 선커밋된 뒤 청구를 호출하고 그 결과를 반영한다")
        fun success() {
            val prepared = created()
            every { transactionService.prepareRenewalOrderInNewTransaction(1L, any()) } returns prepared
            every { paymentGateway.charge(any()) } returns PaymentResult(true, "tx-1", null, null, null)
            every {
                transactionService.applyRenewalChargeResultInNewTransaction(any(), any(), any(), any(), any())
            } returns Unit

            service.renewInNewTransaction(1L)

            verify { transactionService.prepareRenewalOrderInNewTransaction(1L, any()) }
            verify { paymentGateway.charge(match { it.billingKeyValue == "bk-value" && it.orderKey == "order-pub" }) }
            verify { transactionService.applyRenewalChargeResultInNewTransaction(1L, 10L, 9900, any(), any()) }
        }
    }
}
