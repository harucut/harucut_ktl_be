package com.harucut.coupon.service

import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.repository.UserCouponRepository
import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import org.springframework.stereotype.Component
import java.time.LocalDateTime

// 현 주기 후로 예약된 무료 grant 쿠폰을 소비해 구독에 활성화한다.
// 갱신/만료 배치가 공유하는 컴포넌트 — 호출부가 subscription.reservedGrantCouponId != null 을 이미 확인한 상태로 호출한다.
@Component
class GrantActivationService(
    private val userCouponRepository: UserCouponRepository
) {

    fun activate(subscription: UserSubscription, now: LocalDateTime) {
        val userCoupon = userCouponRepository.findById(subscription.reservedGrantCouponId!!)
            .orElseThrow { BusinessException(CouponErrorCode.COUPON_NOT_FOUND) }

        subscription.activateGrant(userCoupon.coupon.grantTier, now, now.plusMonths(1))
        userCoupon.markRedeemed()
        subscription.clearReservedGrant()
    }
}
