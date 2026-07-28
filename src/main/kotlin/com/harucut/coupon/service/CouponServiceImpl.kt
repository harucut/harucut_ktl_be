package com.harucut.coupon.service

import com.harucut.coupon.dto.MyCouponResponse
import com.harucut.coupon.dto.RedeemResultResponse
import com.harucut.coupon.entity.UserCoupon
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.repository.CouponRepository
import com.harucut.coupon.repository.UserCouponRepository
import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class CouponServiceImpl(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) : CouponService {

    // 코드 사용: 무료 구독(BASIC)이면 즉시 개시, tier 접근 중이면 현 주기 후 예약
    override fun redeem(userId: Long, code: String): RedeemResultResponse {
        val coupon = couponRepository.findByCode(code) ?: throw BusinessException(CouponErrorCode.COUPON_NOT_FOUND)
        val now = LocalDateTime.now(clock)

        if (!coupon.isRedeemable(now)) {
            throw BusinessException(CouponErrorCode.COUPON_INACTIVE)
        }
        val maxRedemptions = coupon.maxRedemptions
        if (maxRedemptions != null && userCouponRepository.countByCouponId(coupon.id!!) >= maxRedemptions) {
            throw BusinessException(CouponErrorCode.COUPON_EXHAUSTED)
        }
        if (userCouponRepository.existsByUserIdAndCouponId(userId, coupon.id!!)) {
            throw BusinessException(CouponErrorCode.COUPON_ALREADY_REDEEMED)
        }

        val subscription = userSubscriptionRepository.findByUserId(userId)
            ?: userSubscriptionRepository.save(UserSubscription.createDefault(userRepository.getReferenceById(userId)))

        if (subscription.reservedGrantCouponId != null) {
            throw BusinessException(CouponErrorCode.RESERVATION_EXISTS)
        }

        return if (subscription.effectiveTier(now) == PlanTier.BASIC) {
            val start = now
            val end = now.plusMonths(1)
            subscription.activateGrant(coupon.grantTier, start, end)
            userCouponRepository.save(UserCoupon.redeemed(coupon, userId, now))
            RedeemResultResponse(applied = true, grantTier = coupon.grantTier, startsAt = start, endsAt = end)
        } else {
            val userCoupon = userCouponRepository.save(UserCoupon.reserved(coupon, userId, now))
            subscription.reserveGrant(userCoupon.id!!)
            val start = subscription.currentPeriodEnd ?: now
            val end = start.plusMonths(1)
            RedeemResultResponse(applied = false, grantTier = coupon.grantTier, startsAt = start, endsAt = end)
        }
    }

    @Transactional(readOnly = true)
    override fun getMyCoupons(userId: Long): List<MyCouponResponse> =
        userCouponRepository.findAllByUserId(userId).map { MyCouponResponse.from(it) }
}
