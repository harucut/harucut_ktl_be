package com.harucut.payment.batch.service

import com.harucut.payment.entity.BillingKey
import com.harucut.payment.entity.Payment
import com.harucut.payment.entity.PaymentOrder
import com.harucut.payment.gateway.dto.PaymentResult
import com.harucut.payment.repository.PaymentOrderRepository
import com.harucut.payment.repository.PaymentRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.config.PlanPricingProperties
import com.harucut.user.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class SubscriptionRenewalTransactionServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val paymentOrderRepository = mockk<PaymentOrderRepository>()
    private val paymentRepository = mockk<PaymentRepository>()
    private val planPricingProperties = PlanPricingProperties()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)
    private val now = java.time.LocalDateTime.now(fixedClock)

    private val service = SubscriptionRenewalTransactionService(
        userSubscriptionRepository, paymentOrderRepository, paymentRepository, planPricingProperties
    )

    private fun user(): User = mockk(relaxed = true) { every { publicId } returns "pub-1" }
    private fun billingKey(value: String = "bk-value"): BillingKey = mockk(relaxed = true) { every { billingKeyValue } returns value }

    private fun subscription(billingKey: BillingKey? = billingKey()): UserSubscription = mockk(relaxed = true) {
        every { this@mockk.billingKey } returns billingKey
        every { planTier } returns PlanTier.PLUS
        every { user } returns user()
    }

    @Nested
    inner class PrepareRenewalOrderInNewTransaction {

        @Test
        @DisplayName("구독이 존재하지 않으면 NotFound를 반환하고 주문을 만들지 않는다")
        fun notFound() {
            every { userSubscriptionRepository.findById(1L) } returns Optional.empty()

            val result = service.prepareRenewalOrderInNewTransaction(1L, now)

            assertThat(result).isEqualTo(SubscriptionRenewalTransactionService.RenewalPreparation.NotFound)
            verify(exactly = 0) { paymentOrderRepository.save(any()) }
        }

        @Test
        @DisplayName("빌링키가 없으면 PAST_DUE로 전환하고 NoBillingKey를 반환한다")
        fun noBillingKey() {
            val sub = subscription(billingKey = null)
            every { userSubscriptionRepository.findById(1L) } returns Optional.of(sub)

            val result = service.prepareRenewalOrderInNewTransaction(1L, now)

            assertThat(result).isEqualTo(SubscriptionRenewalTransactionService.RenewalPreparation.NoBillingKey)
            verify { sub.markPastDue() }
            verify(exactly = 0) { paymentOrderRepository.save(any()) }
        }

        @Test
        @DisplayName("같은 결제 주기에 이미 처리된 주문이 있으면 AlreadyProcessed를 반환한다(멱등)")
        fun alreadyProcessed() {
            val sub = subscription()
            every { userSubscriptionRepository.findById(1L) } returns Optional.of(sub)
            every { paymentOrderRepository.existsByIdempotencyKey("renewal:1:202606") } returns true

            val result = service.prepareRenewalOrderInNewTransaction(1L, now)

            assertThat(result).isEqualTo(SubscriptionRenewalTransactionService.RenewalPreparation.AlreadyProcessed)
            verify(exactly = 0) { paymentOrderRepository.save(any()) }
        }

        @Test
        @DisplayName("정상 준비되면 주문을 생성하고 Created를 반환한다")
        fun created() {
            val sub = subscription()
            val order = mockk<PaymentOrder>(relaxed = true) {
                every { id } returns 10L
                every { publicId } returns "order-pub"
            }
            every { userSubscriptionRepository.findById(1L) } returns Optional.of(sub)
            every { paymentOrderRepository.existsByIdempotencyKey("renewal:1:202606") } returns false
            every { paymentOrderRepository.save(any()) } returns order

            val result = service.prepareRenewalOrderInNewTransaction(1L, now)
                as SubscriptionRenewalTransactionService.RenewalPreparation.Created

            assertThat(result.orderId).isEqualTo(10L)
            assertThat(result.orderPublicId).isEqualTo("order-pub")
            assertThat(result.billingKeyValue).isEqualTo("bk-value")
            assertThat(result.planTier).isEqualTo(PlanTier.PLUS)
        }
    }

    @Nested
    inner class ApplyRenewalChargeResultInNewTransaction {

        @Test
        @DisplayName("청구에 성공하면 결제를 승인하고 구독을 다음 주기로 갱신한다")
        fun success() {
            val order = mockk<PaymentOrder>(relaxed = true)
            val sub = mockk<UserSubscription>(relaxed = true)
            val payment = mockk<Payment>(relaxed = true)
            every { paymentOrderRepository.getReferenceById(10L) } returns order
            every { userSubscriptionRepository.getReferenceById(1L) } returns sub
            every { paymentRepository.save(any()) } returns payment

            service.applyRenewalChargeResultInNewTransaction(
                1L, 10L, 9900, PaymentResult(true, "tx-1", null, null, null), now
            )

            verify { payment.approve("tx-1", any()) }
            verify { order.markPaid() }
            verify { sub.renew(any(), any()) }
        }

        @Test
        @DisplayName("청구에 실패하면 결제를 실패 처리하고 구독을 PAST_DUE로 전환한다")
        fun failure() {
            val order = mockk<PaymentOrder>(relaxed = true)
            val sub = mockk<UserSubscription>(relaxed = true)
            val payment = mockk<Payment>(relaxed = true)
            every { paymentOrderRepository.getReferenceById(10L) } returns order
            every { userSubscriptionRepository.getReferenceById(1L) } returns sub
            every { paymentRepository.save(any()) } returns payment

            service.applyRenewalChargeResultInNewTransaction(
                1L, 10L, 9900, PaymentResult(false, null, null, "DECLINED", "거절"), now
            )

            verify { payment.fail("DECLINED", "거절") }
            verify { order.markFailed() }
            verify { sub.markPastDue() }
        }
    }
}
