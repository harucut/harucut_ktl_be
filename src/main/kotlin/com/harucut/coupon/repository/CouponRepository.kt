package com.harucut.coupon.repository

import com.harucut.coupon.entity.Coupon
import org.springframework.data.jpa.repository.JpaRepository

interface CouponRepository : JpaRepository<Coupon, Long> {

    fun findByCode(code: String): Coupon?

    fun existsByCode(code: String): Boolean

    fun findByPublicId(publicId: String): Coupon?
}
