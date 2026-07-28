package com.harucut.coupon.service

import com.harucut.coupon.entity.Coupon
import com.harucut.coupon.entity.UserCoupon
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.repository.UserCouponRepository
import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

class GrantActivationServiceTest {

    private val userCouponRepository = mockk<UserCouponRepository>()
    private val service = GrantActivationService(userCouponRepository)
    private val now = LocalDateTime.now()

    @Nested
    inner class Activate {

        @Test
        @DisplayName("예약된 쿠폰의 grant tier로 구독을 활성화하고 예약을 소비/해제한다")
        fun success() {
            val coupon = mockk<Coupon>(relaxed = true) { every { grantTier } returns PlanTier.PRO }
            val userCoupon = mockk<UserCoupon>(relaxed = true) { every { this@mockk.coupon } returns coupon }
            val subscription = mockk<UserSubscription>(relaxed = true) { every { reservedGrantCouponId } returns 10L }
            every { userCouponRepository.findById(10L) } returns Optional.of(userCoupon)

            service.activate(subscription, now)

            verify { subscription.activateGrant(PlanTier.PRO, now, now.plusMonths(1)) }
            verify { userCoupon.markRedeemed() }
            verify { subscription.clearReservedGrant() }
        }

        @Test
        @DisplayName("예약된 UserCoupon을 찾을 수 없으면 COUPON_NOT_FOUND 예외를 던진다")
        fun notFound() {
            val subscription = mockk<UserSubscription>(relaxed = true) { every { reservedGrantCouponId } returns 10L }
            every { userCouponRepository.findById(10L) } returns Optional.empty()

            assertThatThrownBy { service.activate(subscription, now) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)

            verify(exactly = 0) { subscription.activateGrant(any(), any(), any()) }
        }
    }
}
