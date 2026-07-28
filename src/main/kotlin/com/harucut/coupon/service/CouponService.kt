package com.harucut.coupon.service

import com.harucut.coupon.dto.MyCouponResponse
import com.harucut.coupon.dto.RedeemResultResponse

interface CouponService {
    fun redeem(userId: Long, code: String): RedeemResultResponse
    fun getMyCoupons(userId: Long): List<MyCouponResponse>
}
