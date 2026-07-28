package com.harucut.coupon.entity

import com.harucut.coupon.enums.UserCouponStatus
import com.harucut.subscription.plan.PlanTier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserCouponTest {

    private fun coupon(): Coupon =
        Coupon(name = "가입 축하 PRO 1개월", code = "WELCOME-PRO", grantTier = PlanTier.PRO)

    @Nested
    inner class Reserved {

        @Test
        @DisplayName("reserved 팩토리는 RESERVED 상태로 생성한다")
        fun success() {
            val now = LocalDateTime.now()

            val uc = UserCoupon.reserved(coupon(), userId = 1L, now = now)

            assertThat(uc.status).isEqualTo(UserCouponStatus.RESERVED)
            assertThat(uc.userId).isEqualTo(1L)
            assertThat(uc.redeemedAt).isEqualTo(now)
        }
    }

    @Nested
    inner class Redeemed {

        @Test
        @DisplayName("redeemed 팩토리는 REDEEMED 상태로 생성한다")
        fun success() {
            val now = LocalDateTime.now()

            val uc = UserCoupon.redeemed(coupon(), userId = 1L, now = now)

            assertThat(uc.status).isEqualTo(UserCouponStatus.REDEEMED)
            assertThat(uc.userId).isEqualTo(1L)
            assertThat(uc.redeemedAt).isEqualTo(now)
        }
    }

    @Nested
    inner class MarkRedeemed {

        @Test
        @DisplayName("RESERVED 상태를 REDEEMED로 전환한다")
        fun success() {
            val uc = UserCoupon.reserved(coupon(), userId = 1L, now = LocalDateTime.now())
            val reservedAt = uc.redeemedAt

            uc.markRedeemed()

            assertThat(uc.status).isEqualTo(UserCouponStatus.REDEEMED)
            assertThat(uc.redeemedAt).isEqualTo(reservedAt)
        }
    }
}
