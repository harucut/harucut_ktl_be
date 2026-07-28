package com.harucut.coupon.service

import com.harucut.coupon.dto.CouponResponse
import com.harucut.coupon.entity.Coupon
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.repository.CouponRepository
import com.harucut.coupon.repository.UserCouponRepository
import com.harucut.exception.BusinessException
import com.harucut.subscription.plan.PlanTier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class CouponAdminServiceImpl(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) : CouponAdminService {

    // 코드 기반 무료 쿠폰 생성 (PLUS/PRO만 허용, 코드 중복 불가)
    override fun createCoupon(
        name: String,
        code: String,
        grantTier: PlanTier,
        maxRedemptions: Int?,
        validUntil: LocalDateTime?
    ) {
        if (grantTier == PlanTier.BASIC) {
            throw BusinessException(CouponErrorCode.INVALID_GRANT_TIER)
        }
        if (couponRepository.existsByCode(code)) {
            throw BusinessException(CouponErrorCode.COUPON_CODE_DUPLICATED)
        }

        couponRepository.save(
            Coupon(name = name, code = code, grantTier = grantTier, maxRedemptions = maxRedemptions, validUntil = validUntil)
        )
    }

    // 쿠폰 비활성화
    override fun deactivateCoupon(publicId: String) {
        getCouponByPublicId(publicId).deactivate()
    }

    // 전체 쿠폰 목록 (누적 사용 수 포함) — 사용 수는 GROUP BY 집계 쿼리 1회로 조회해 N+1을 방지한다.
    @Transactional(readOnly = true)
    override fun listCoupons(): List<CouponResponse> {
        val redeemedCounts = userCouponRepository.countGroupedByCouponId().associate { it.couponId to it.cnt }
        return couponRepository.findAll().map { coupon ->
            CouponResponse.from(coupon, redeemedCounts[coupon.id!!] ?: 0L)
        }
    }

    private fun getCouponByPublicId(publicId: String): Coupon =
        couponRepository.findByPublicId(publicId) ?: throw BusinessException(CouponErrorCode.COUPON_NOT_FOUND)
}
