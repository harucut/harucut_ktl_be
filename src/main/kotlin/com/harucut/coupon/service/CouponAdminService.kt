package com.harucut.coupon.service

import com.harucut.coupon.dto.CouponResponse
import com.harucut.subscription.plan.PlanTier
import java.time.LocalDateTime

interface CouponAdminService {
    fun createCoupon(name: String, code: String, grantTier: PlanTier, maxRedemptions: Int?, validUntil: LocalDateTime?)
    fun deactivateCoupon(publicId: String)
    fun listCoupons(): List<CouponResponse>
}
