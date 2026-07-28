package com.harucut.subscription.entity

import com.harucut.payment.entity.BillingKey
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserSubscriptionTest {

    private fun user(): User = mockk(relaxed = true)
    private fun billingKey(): BillingKey = mockk(relaxed = true)

    @Nested
    inner class CreateDefault {

        @Test
        @DisplayName("기본 구독은 BASIC 요금제로 생성된다")
        fun success() {
            val sub = UserSubscription.createDefault(user())

            assertThat(sub.planTier).isEqualTo(PlanTier.BASIC)
        }
    }

    @Nested
    inner class ChangePlan {

        @Test
        @DisplayName("요금제 단계를 변경한다")
        fun success() {
            val sub = UserSubscription.createDefault(user())

            sub.changePlan(PlanTier.PRO)

            assertThat(sub.planTier).isEqualTo(PlanTier.PRO)
        }
    }

    @Nested
    inner class ActivatePaid {

        @Test
        @DisplayName("결제 성공 시 요금제·기간·자동갱신·빌링키를 활성화한다")
        fun success() {
            val sub = UserSubscription.createDefault(user())
            val start = LocalDateTime.now()
            val end = start.plusMonths(1)
            val key = billingKey()

            sub.activatePaid(PlanTier.PRO, start, end, key)

            assertThat(sub.planTier).isEqualTo(PlanTier.PRO)
            assertThat(sub.status).isEqualTo(SubscriptionStatus.ACTIVE)
            assertThat(sub.currentPeriodStart).isEqualTo(start)
            assertThat(sub.currentPeriodEnd).isEqualTo(end)
            assertThat(sub.autoRenew).isTrue()
            assertThat(sub.billingKey).isEqualTo(key)
        }
    }

    @Nested
    inner class Renew {

        @Test
        @DisplayName("정기결제 성공 시 다음 결제 주기로 갱신하고 ACTIVE로 되돌린다")
        fun success() {
            val sub = UserSubscription.createDefault(user())
            val start = LocalDateTime.now()
            sub.activatePaid(PlanTier.PLUS, start, start.plusMonths(1), billingKey())
            sub.markPastDue()

            val nextStart = start.plusMonths(1)
            val nextEnd = start.plusMonths(2)
            sub.renew(nextStart, nextEnd)

            assertThat(sub.status).isEqualTo(SubscriptionStatus.ACTIVE)
            assertThat(sub.currentPeriodStart).isEqualTo(nextStart)
            assertThat(sub.currentPeriodEnd).isEqualTo(nextEnd)
        }
    }

    @Nested
    inner class MarkPastDue {

        @Test
        @DisplayName("정기결제 실패 시 PAST_DUE 상태가 된다")
        fun success() {
            val sub = UserSubscription.createDefault(user())
            sub.activatePaid(PlanTier.PLUS, LocalDateTime.now(), LocalDateTime.now().plusMonths(1), billingKey())

            sub.markPastDue()

            assertThat(sub.status).isEqualTo(SubscriptionStatus.PAST_DUE)
        }
    }

    @Nested
    inner class CancelAutoRenew {

        @Test
        @DisplayName("해지 예약 시 autoRenew는 꺼지지만 만료 전까지 요금제는 유지된다")
        fun success() {
            val end = LocalDateTime.now().plusDays(10)
            val sub = UserSubscription.createDefault(user())
            sub.activatePaid(PlanTier.PRO, LocalDateTime.now(), end, billingKey())

            sub.cancelAutoRenew()

            assertThat(sub.autoRenew).isFalse()
            assertThat(sub.status).isEqualTo(SubscriptionStatus.CANCELED)
            assertThat(sub.planTier).isEqualTo(PlanTier.PRO)
            assertThat(sub.currentPeriodEnd).isEqualTo(end)
        }
    }

    @Nested
    inner class ExpireToFree {

        @Test
        @DisplayName("만료 처리 시 BASIC으로 강등하고 결제 정보를 정리한다")
        fun success() {
            val sub = UserSubscription.createDefault(user())
            sub.activatePaid(PlanTier.PRO, LocalDateTime.now(), LocalDateTime.now().plusMonths(1), billingKey())

            sub.expireToFree()

            assertThat(sub.planTier).isEqualTo(PlanTier.BASIC)
            assertThat(sub.status).isEqualTo(SubscriptionStatus.EXPIRED)
            assertThat(sub.currentPeriodStart).isNull()
            assertThat(sub.currentPeriodEnd).isNull()
            assertThat(sub.autoRenew).isFalse()
            assertThat(sub.billingKey).isNull()
        }
    }

    @Nested
    inner class ActivateGrant {

        @Test
        @DisplayName("쿠폰 사용 시 요금제·기간을 GRANTED로 개시하고 자동갱신은 켜지 않으며 빌링키는 그대로 둔다")
        fun success() {
            val sub = UserSubscription.createDefault(user())
            val key = billingKey()
            sub.activatePaid(PlanTier.PLUS, LocalDateTime.now(), LocalDateTime.now().plusMonths(1), key)
            val start = LocalDateTime.now()
            val end = start.plusMonths(1)

            sub.activateGrant(PlanTier.PRO, start, end)

            assertThat(sub.planTier).isEqualTo(PlanTier.PRO)
            assertThat(sub.status).isEqualTo(SubscriptionStatus.GRANTED)
            assertThat(sub.currentPeriodStart).isEqualTo(start)
            assertThat(sub.currentPeriodEnd).isEqualTo(end)
            assertThat(sub.autoRenew).isFalse()
            assertThat(sub.billingKey).isEqualTo(key)
        }
    }

    @Nested
    inner class ReserveGrant {

        @Test
        @DisplayName("쿠폰을 예약하면 예약 쿠폰 id가 설정되고, 해제하면 null이 된다")
        fun success() {
            val sub = UserSubscription.createDefault(user())

            sub.reserveGrant(10L)
            assertThat(sub.reservedGrantCouponId).isEqualTo(10L)

            sub.clearReservedGrant()
            assertThat(sub.reservedGrantCouponId).isNull()
        }
    }

    @Nested
    inner class EffectiveTier {

        @Test
        @DisplayName("BASIC은 항상 BASIC을 반환한다")
        fun basic() {
            val sub = UserSubscription.createDefault(user())

            assertThat(sub.effectiveTier(LocalDateTime.now())).isEqualTo(PlanTier.BASIC)
        }

        @Test
        @DisplayName("결제 주기 만료 전이면 결제한 요금제를 반환한다")
        fun beforeExpiry() {
            val now = LocalDateTime.now()
            val sub = UserSubscription.createDefault(user())
            sub.activatePaid(PlanTier.PRO, now.minusDays(1), now.plusDays(1), billingKey())

            assertThat(sub.effectiveTier(now)).isEqualTo(PlanTier.PRO)
        }

        @Test
        @DisplayName("결제 주기가 지났으면 배치가 아직 강등하지 않았어도 BASIC을 반환한다")
        fun afterExpiry() {
            val now = LocalDateTime.now()
            val sub = UserSubscription.createDefault(user())
            sub.activatePaid(PlanTier.PRO, now.minusDays(40), now.minusDays(10), billingKey())

            assertThat(sub.effectiveTier(now)).isEqualTo(PlanTier.BASIC)
        }

        @Test
        @DisplayName("무료 grant 기간 중이면 grant된 요금제를 반환한다")
        fun duringGrant() {
            val now = LocalDateTime.now()
            val sub = UserSubscription.createDefault(user())
            sub.activateGrant(PlanTier.PRO, now.minusDays(1), now.plusDays(1))

            assertThat(sub.effectiveTier(now)).isEqualTo(PlanTier.PRO)
        }

        @Test
        @DisplayName("무료 grant 기간이 지났으면 배치가 아직 강등하지 않았어도 BASIC을 반환한다")
        fun afterGrantExpiry() {
            val now = LocalDateTime.now()
            val sub = UserSubscription.createDefault(user())
            sub.activateGrant(PlanTier.PRO, now.minusDays(40), now.minusDays(10))

            assertThat(sub.effectiveTier(now)).isEqualTo(PlanTier.BASIC)
        }
    }
}
