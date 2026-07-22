package com.harucut.subscription.repository

import com.harucut.payment.entity.BillingKey
import com.harucut.payment.enums.PgProvider
import com.harucut.payment.repository.BillingKeyRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

// 관리자 구독 스냅샷 집계 쿼리를 실제 H2 스키마로 검증한다.
@DataJpaTest
class UserSubscriptionRepositoryTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var billingKeyRepository: BillingKeyRepository

    @Autowired
    lateinit var userSubscriptionRepository: UserSubscriptionRepository

    private fun user(email: String): User = userRepository.save(
        User(
            provider = Provider.HARUCUT,
            userRole = UserRole.ROLE_USER,
            email = email,
            username = "tester",
            profileImageUrl = "resources/defaults/userDefaultImage.png",
            userStatus = UserStatus.ACTIVE
        )
    )

    private fun activatedSubscription(email: String, tier: PlanTier, periodStart: LocalDateTime, periodEnd: LocalDateTime): UserSubscription {
        val user = user(email)
        val billingKey = billingKeyRepository.save(BillingKey(user, PgProvider.MOCK, "bk-$email"))
        val subscription = UserSubscription.createDefault(user)
        subscription.activatePaid(tier, periodStart, periodEnd, billingKey)
        return subscription
    }

    @Test
    @DisplayName("status별, 자동갱신 on/off 수를 정확히 센다")
    fun countsByStatusAndAutoRenew() {
        val now = LocalDateTime.of(2026, 7, 1, 0, 0)

        val plusActive = activatedSubscription("plus-active@harucut.com", PlanTier.PLUS, now, now.plusMonths(1))
        val proActive = activatedSubscription("pro-active@harucut.com", PlanTier.PRO, now, now.plusMonths(1))
        val canceledFuture = activatedSubscription("canceled-future@harucut.com", PlanTier.PLUS, now, now.plusMonths(1))
        canceledFuture.cancelAutoRenew()
        val canceledExpired = activatedSubscription("canceled-expired@harucut.com", PlanTier.PLUS, now.minusMonths(2), now.minusDays(1))
        canceledExpired.cancelAutoRenew()
        val basicFree = UserSubscription.createDefault(user("basic-free@harucut.com"))

        userSubscriptionRepository.saveAll(listOf(plusActive, proActive, canceledFuture, canceledExpired, basicFree))
        userSubscriptionRepository.flush()

        // basicFree도 결제 이력 없이 기본 상태가 ACTIVE(무료 활성)이므로 3건이 된다.
        assertThat(userSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)).isEqualTo(3)
        assertThat(userSubscriptionRepository.countByStatus(SubscriptionStatus.CANCELED)).isEqualTo(2)
        assertThat(userSubscriptionRepository.countByAutoRenew(true)).isEqualTo(2)
        assertThat(userSubscriptionRepository.countByAutoRenew(false)).isEqualTo(3)
    }

    @Test
    @DisplayName("tier별 '현재 유료 접근 중' 구독 수는 ACTIVE와 만료 전 CANCELED를 포함하고, 만료된 CANCELED는 제외한다")
    fun countsCurrentlyPaidAccessibleByTier() {
        val now = LocalDateTime.of(2026, 7, 1, 0, 0)
        val paidAccessStatuses = listOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED)

        val plusActive = activatedSubscription("plus-active2@harucut.com", PlanTier.PLUS, now, now.plusMonths(1))
        val proActive = activatedSubscription("pro-active2@harucut.com", PlanTier.PRO, now, now.plusMonths(1))
        // 해지 예약(CANCELED)이지만 currentPeriodEnd(만료)까지는 유료 접근이 유지되는 구독
        val canceledFuture = activatedSubscription("canceled-future2@harucut.com", PlanTier.PLUS, now, now.plusMonths(1))
        canceledFuture.cancelAutoRenew()
        // CANCELED이면서 이미 currentPeriodEnd가 지난(만료된) 구독 — 유료 접근 대상에서 제외돼야 함
        val canceledExpired = activatedSubscription("canceled-expired2@harucut.com", PlanTier.PLUS, now.minusMonths(2), now.minusDays(1))
        canceledExpired.cancelAutoRenew()
        val basicFree = UserSubscription.createDefault(user("basic-free2@harucut.com"))

        userSubscriptionRepository.saveAll(listOf(plusActive, proActive, canceledFuture, canceledExpired, basicFree))
        userSubscriptionRepository.flush()

        assertThat(
            userSubscriptionRepository.countByStatusInAndPlanTierAndCurrentPeriodEndAfter(paidAccessStatuses, PlanTier.PLUS, now)
        ).isEqualTo(2) // plusActive + canceledFuture (canceledExpired는 제외)
        assertThat(
            userSubscriptionRepository.countByStatusInAndPlanTierAndCurrentPeriodEndAfter(paidAccessStatuses, PlanTier.PRO, now)
        ).isEqualTo(1)
    }
}
