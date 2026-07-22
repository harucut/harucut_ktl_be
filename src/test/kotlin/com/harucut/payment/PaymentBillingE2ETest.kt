package com.harucut.payment

import com.harucut.payment.batch.scheduler.SubscriptionExpirationScheduler
import com.harucut.payment.batch.scheduler.SubscriptionRenewalScheduler
import com.harucut.payment.batch.service.SubscriptionRenewalBatchService
import com.harucut.payment.config.PaymentProperties
import com.harucut.payment.enums.OrderStatus
import com.harucut.payment.enums.OrderType
import com.harucut.payment.enums.PaymentStatus
import com.harucut.payment.repository.PaymentOrderRepository
import com.harucut.payment.repository.PaymentRepository
import com.harucut.payment.service.PaymentService
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.support.MutableClock
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

// Mock 게이트웨이 + 전진 가능한 Clock으로 "구독 → 만료 → 재청구 → 갱신" 전체 흐름을 검증하는 e2e 테스트.
@SpringBootTest
@Import(PaymentBillingE2ETest.ClockTestConfig::class)
class PaymentBillingE2ETest {

    @TestConfiguration
    class ClockTestConfig {
        @Bean
        @Primary
        fun testClock(): MutableClock =
            MutableClock(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
    }

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userSubscriptionRepository: UserSubscriptionRepository

    @Autowired
    lateinit var paymentOrderRepository: PaymentOrderRepository

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    @Autowired
    lateinit var subscriptionRenewalScheduler: SubscriptionRenewalScheduler

    @Autowired
    lateinit var subscriptionRenewalBatchService: SubscriptionRenewalBatchService

    @Autowired
    lateinit var subscriptionExpirationScheduler: SubscriptionExpirationScheduler

    @Autowired
    lateinit var paymentProperties: PaymentProperties

    @Autowired
    lateinit var testClock: MutableClock

    @AfterEach
    fun resetMockFailure() {
        paymentProperties.mock.failCharge = false
    }

    private fun createUser(email: String): User =
        userRepository.save(
            User(
                provider = Provider.HARUCUT,
                userRole = UserRole.ROLE_USER,
                email = email,
                username = "tester",
                profileImageUrl = "resources/defaults/userDefaultImage.png",
                userStatus = UserStatus.ACTIVE
            )
        )

    @Test
    @DisplayName("구독 → 만료 도래 → 스케줄러 재청구 → 갱신까지 성공한다")
    fun subscribeThenRenewOnSchedule() {
        val user = createUser("renew@harucut.com")

        val subscribed = paymentService.subscribe(user.id!!, PlanTier.PRO, "customer-renew", "auth-renew")
        assertThat(subscribed.planTier).isEqualTo("PRO")
        assertThat(subscribed.status).isEqualTo("ACTIVE")

        val firstPeriodEnd = userSubscriptionRepository.findByUserId(user.id!!)!!.currentPeriodEnd!!

        testClock.advanceBy(Duration.ofDays(32))
        subscriptionRenewalScheduler.run()

        val renewed = userSubscriptionRepository.findByUserId(user.id!!)!!
        assertThat(renewed.status).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(renewed.currentPeriodEnd).isAfter(firstPeriodEnd)

        val renewalOrder = paymentOrderRepository.findAll()
            .first { it.user.id == user.id && it.orderType == OrderType.RENEWAL }
        assertThat(renewalOrder.status).isEqualTo(OrderStatus.PAID)

        val renewalPayments = paymentRepository.findAll().filter { it.order.id == renewalOrder.id }
        assertThat(renewalPayments).hasSize(1)
        assertThat(renewalPayments.first().status).isEqualTo(PaymentStatus.APPROVED)
    }

    @Test
    @DisplayName("정기결제 실패 → PAST_DUE → 유예기간 초과 후 BASIC으로 강등된다")
    fun renewalFailureLeadsToExpiration() {
        val user = createUser("fail@harucut.com")
        paymentService.subscribe(user.id!!, PlanTier.PLUS, "customer-fail", "auth-fail")

        testClock.advanceBy(Duration.ofDays(32))
        paymentProperties.mock.failCharge = true
        subscriptionRenewalScheduler.run()

        val pastDue = userSubscriptionRepository.findByUserId(user.id!!)!!
        assertThat(pastDue.status).isEqualTo(SubscriptionStatus.PAST_DUE)
        assertThat(pastDue.planTier).isEqualTo(PlanTier.PLUS)

        testClock.advanceBy(Duration.ofDays(paymentProperties.graceDays + 1))
        subscriptionExpirationScheduler.run()

        val expired = userSubscriptionRepository.findByUserId(user.id!!)!!
        assertThat(expired.status).isEqualTo(SubscriptionStatus.EXPIRED)
        assertThat(expired.planTier).isEqualTo(PlanTier.BASIC)
    }

    @Test
    @DisplayName("같은 결제 주기에 재청구 배치를 두 번 실행해도 결제는 1건만 생성된다(멱등)")
    fun renewalIsIdempotentWithinSamePeriod() {
        val user = createUser("idem@harucut.com")
        paymentService.subscribe(user.id!!, PlanTier.PRO, "customer-idem", "auth-idem")

        testClock.advanceBy(Duration.ofDays(32))
        val subscriptionId = userSubscriptionRepository.findByUserId(user.id!!)!!.id!!

        subscriptionRenewalBatchService.renewInNewTransaction(subscriptionId)
        subscriptionRenewalBatchService.renewInNewTransaction(subscriptionId)

        val renewalOrders = paymentOrderRepository.findAll()
            .filter { it.user.id == user.id && it.orderType == OrderType.RENEWAL }
        assertThat(renewalOrders).hasSize(1)

        val renewalPayments = paymentRepository.findAll().filter { it.order.id == renewalOrders.first().id }
        assertThat(renewalPayments).hasSize(1)
    }
}
