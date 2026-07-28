package com.harucut.coupon.repository

import com.harucut.coupon.entity.UserCoupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserCouponRepository : JpaRepository<UserCoupon, Long> {

    fun findAllByUserId(userId: Long): List<UserCoupon>

    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    // redeem 시 사용 상한 체크
    fun countByCouponId(couponId: Long): Long

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserCoupon uc WHERE uc.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)

    // 관리자 쿠폰 목록: 쿠폰별 누적 사용 수를 1회 집계 쿼리로 조회 (N+1 방지)
    @Query("SELECT uc.coupon.id AS couponId, COUNT(uc) AS cnt FROM UserCoupon uc GROUP BY uc.coupon.id")
    fun countGroupedByCouponId(): List<CouponRedeemCount>
}

interface CouponRedeemCount {
    val couponId: Long
    val cnt: Long
}
