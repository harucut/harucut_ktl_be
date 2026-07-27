package com.harucut.coupon.entity

import com.harucut.subscription.plan.PlanTier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CouponTest {

    private fun coupon(validUntil: LocalDateTime? = null): Coupon =
        Coupon(name = "가입 축하 PRO 1개월", code = "WELCOME-PRO", grantTier = PlanTier.PRO, validUntil = validUntil)

    @Nested
    inner class IsRedeemable {

        @Test
        @DisplayName("활성 + 무기한이면 true를 반환한다")
        fun activeUnlimited() {
            val c = coupon()

            assertThat(c.isRedeemable(LocalDateTime.now())).isTrue()
        }

        @Test
        @DisplayName("활성 + 마감일이 미래면 true를 반환한다")
        fun activeFutureValidUntil() {
            val c = coupon(validUntil = LocalDateTime.now().plusDays(1))

            assertThat(c.isRedeemable(LocalDateTime.now())).isTrue()
        }

        @Test
        @DisplayName("활성 + 마감일이 과거면 false를 반환한다")
        fun activePastValidUntil() {
            val c = coupon(validUntil = LocalDateTime.now().minusDays(1))

            assertThat(c.isRedeemable(LocalDateTime.now())).isFalse()
        }

        @Test
        @DisplayName("비활성이면 false를 반환한다")
        fun inactive() {
            val c = coupon()
            c.deactivate()

            assertThat(c.isRedeemable(LocalDateTime.now())).isFalse()
        }
    }

    @Nested
    inner class Deactivate {

        @Test
        @DisplayName("비활성화하면 active가 false가 된다")
        fun success() {
            val c = coupon()

            c.deactivate()

            assertThat(c.active).isFalse()
        }
    }
}
