package com.harucut.payment.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.payment.enums.PgProvider
import com.harucut.payment.exception.PaymentErrorCode
import com.harucut.payment.gateway.PaymentGateway
import com.harucut.payment.gateway.dto.BillingKeyResult
import com.harucut.payment.gateway.dto.PaymentResult
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.config.PlanPricingProperties
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class PaymentServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val paymentTransactionService = mockk<PaymentTransactionService>()
    private val paymentGateway = mockk<PaymentGateway>()
    private val planPricingProperties = PlanPricingProperties()
    private val clock = Clock.fixed(LocalDateTime.of(2026, 1, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

    private val service = PaymentServiceImpl(
        userRepository, userSubscriptionRepository, paymentTransactionService, paymentGateway, planPricingProperties, clock
    )

    private fun user(): User = mockk(relaxed = true)

    @Nested
    inner class Subscribe {

        @Test
        @DisplayName("BASIC을 요청하면 INVALID_TARGET_PLAN(PAY-007) 예외를 던진다")
        fun basicTarget() {
            assertThatThrownBy { service.subscribe(1L, PlanTier.BASIC, "customer", "auth") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.INVALID_TARGET_PLAN)
        }

        @Test
        @DisplayName("사용자가 없으면 NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            every { userRepository.existsById(1L) } returns false

            assertThatThrownBy { service.subscribe(1L, PlanTier.PLUS, "customer", "auth") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }

        @Test
        @DisplayName("이미 유료 요금제를 구독 중이면 ALREADY_SUBSCRIBED(PAY-003) 예외를 던진다")
        fun alreadySubscribed() {
            every { userRepository.existsById(1L) } returns true
            val active = mockk<UserSubscription>(relaxed = true) {
                every { effectiveTier(any()) } returns PlanTier.PRO
            }
            every { userSubscriptionRepository.findByUserId(1L) } returns active

            assertThatThrownBy { service.subscribe(1L, PlanTier.PLUS, "customer", "auth") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.ALREADY_SUBSCRIBED)
        }

        @Test
        @DisplayName("빌링키 발급에 실패하면 BILLING_KEY_ISSUE_FAILED(PAY-001) 예외를 던진다")
        fun billingKeyIssueFailed() {
            every { userRepository.existsById(1L) } returns true
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { paymentGateway.issueBillingKey(any()) } returns
                BillingKeyResult(false, null, null, "FAILED", "실패")

            assertThatThrownBy { service.subscribe(1L, PlanTier.PLUS, "customer", "auth") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.BILLING_KEY_ISSUE_FAILED)
        }

        @Test
        @DisplayName("청구에 실패하면 PAYMENT_FAILED(PAY-002) 예외를 던진다")
        fun chargeFailed() {
            every { userRepository.existsById(1L) } returns true
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { paymentGateway.issueBillingKey(any()) } returns
                BillingKeyResult(true, "bk-value", "**** 1234", null, null)
            every { paymentGateway.provider() } returns PgProvider.MOCK
            every {
                paymentTransactionService.createInitialOrderInNewTransaction(any(), any(), any(), any(), any(), any(), any())
            } returns PaymentTransactionService.CreatedOrder(orderId = 10L, orderPublicId = "order-pub", billingKeyId = 20L)
            every { paymentGateway.charge(any()) } returns
                PaymentResult(false, null, null, "DECLINED", "거절")
            every {
                paymentTransactionService.applyInitialChargeResultInNewTransaction(any(), any(), any(), any(), any(), any())
            } returns PaymentTransactionService.ChargeApplyResult(success = false, subscription = null)

            assertThatThrownBy { service.subscribe(1L, PlanTier.PLUS, "customer", "auth") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_FAILED)
        }

        @Test
        @DisplayName("성공하면 구독 상태 응답을 반환한다")
        fun success() {
            every { userRepository.existsById(1L) } returns true
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { paymentGateway.issueBillingKey(any()) } returns
                BillingKeyResult(true, "bk-value", "**** 1234", null, null)
            every { paymentGateway.provider() } returns PgProvider.MOCK
            every {
                paymentTransactionService.createInitialOrderInNewTransaction(any(), any(), any(), any(), any(), any(), any())
            } returns PaymentTransactionService.CreatedOrder(orderId = 10L, orderPublicId = "order-pub", billingKeyId = 20L)
            every { paymentGateway.charge(any()) } returns
                PaymentResult(true, "tx-1", LocalDateTime.now(clock), null, null)

            val activated = UserSubscription.createDefault(user()).also {
                it.activatePaid(PlanTier.PLUS, LocalDateTime.now(clock), LocalDateTime.now(clock).plusMonths(1), mockk(relaxed = true))
            }
            every {
                paymentTransactionService.applyInitialChargeResultInNewTransaction(any(), any(), any(), any(), any(), any())
            } returns PaymentTransactionService.ChargeApplyResult(success = true, subscription = activated)

            val response = service.subscribe(1L, PlanTier.PLUS, "customer", "auth")

            assertThat(response.planTier).isEqualTo("PLUS")
            assertThat(response.autoRenew).isTrue()
            verify {
                paymentTransactionService.createInitialOrderInNewTransaction(
                    1L, PlanTier.PLUS, planPricingProperties.priceOf(PlanTier.PLUS), any(), PgProvider.MOCK, "bk-value", "**** 1234"
                )
            }
        }
    }
}
