package com.harucut.coupon.service

import com.harucut.coupon.entity.Coupon
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.repository.CouponRedeemCount
import com.harucut.coupon.repository.CouponRepository
import com.harucut.coupon.repository.UserCouponRepository
import com.harucut.exception.BusinessException
import com.harucut.subscription.plan.PlanTier
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CouponAdminServiceImplTest {

    private val couponRepository = mockk<CouponRepository>()
    private val userCouponRepository = mockk<UserCouponRepository>()
    private val service = CouponAdminServiceImpl(couponRepository, userCouponRepository)

    private fun coupon(code: String = "WELCOME-PRO", tier: PlanTier = PlanTier.PRO): Coupon =
        Coupon(name = "가입 축하 PRO 1개월", code = code, grantTier = tier)

    @Nested
    inner class CreateCoupon {

        @Test
        @DisplayName("PLUS/PRO tier와 중복되지 않은 코드면 쿠폰을 생성한다")
        fun success() {
            every { couponRepository.existsByCode("WELCOME-PRO") } returns false
            val slot = slot<Coupon>()
            every { couponRepository.save(capture(slot)) } answers { slot.captured }

            service.createCoupon("가입 축하 PRO 1개월", "WELCOME-PRO", PlanTier.PRO, 100, null)

            assertThat(slot.captured.name).isEqualTo("가입 축하 PRO 1개월")
            assertThat(slot.captured.code).isEqualTo("WELCOME-PRO")
            assertThat(slot.captured.grantTier).isEqualTo(PlanTier.PRO)
            assertThat(slot.captured.maxRedemptions).isEqualTo(100)
        }

        @Test
        @DisplayName("BASIC tier면 INVALID_GRANT_TIER 예외를 던진다")
        fun basicTier() {
            assertThatThrownBy { service.createCoupon("이름", "CODE", PlanTier.BASIC, null, null) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.INVALID_GRANT_TIER)

            verify(exactly = 0) { couponRepository.save(any()) }
        }

        @Test
        @DisplayName("중복된 코드면 COUPON_CODE_DUPLICATED 예외를 던진다")
        fun duplicatedCode() {
            every { couponRepository.existsByCode("WELCOME-PRO") } returns true

            assertThatThrownBy { service.createCoupon("이름", "WELCOME-PRO", PlanTier.PRO, null, null) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_CODE_DUPLICATED)

            verify(exactly = 0) { couponRepository.save(any()) }
        }
    }

    @Nested
    inner class DeactivateCoupon {

        @Test
        @DisplayName("존재하는 쿠폰을 비활성화한다")
        fun success() {
            val c = coupon()
            every { couponRepository.findByPublicId("public-1") } returns c

            service.deactivateCoupon("public-1")

            assertThat(c.active).isFalse()
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 COUPON_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { couponRepository.findByPublicId("public-1") } returns null

            assertThatThrownBy { service.deactivateCoupon("public-1") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)
        }
    }

    @Nested
    inner class ListCoupons {

        @Test
        @DisplayName("전체 쿠폰 목록을 누적 사용 수와 함께 반환한다")
        fun success() {
            val c1 = mockk<Coupon>(relaxed = true).also {
                every { it.id } returns 1L
                every { it.publicId } returns "public-1"
                every { it.name } returns "쿠폰1"
                every { it.code } returns "CODE1"
                every { it.grantTier } returns PlanTier.PLUS
                every { it.maxRedemptions } returns null
                every { it.validUntil } returns null
                every { it.active } returns true
            }
            val c2 = mockk<Coupon>(relaxed = true).also {
                every { it.id } returns 2L
                every { it.publicId } returns "public-2"
                every { it.name } returns "쿠폰2"
                every { it.code } returns "CODE2"
                every { it.grantTier } returns PlanTier.PRO
                every { it.maxRedemptions } returns 10
                every { it.validUntil } returns null
                every { it.active } returns true
            }
            every { couponRepository.findAll() } returns listOf(c1, c2)
            every { userCouponRepository.countGroupedByCouponId() } returns listOf(
                object : CouponRedeemCount {
                    override val couponId = 1L
                    override val cnt = 3L
                }
            )

            val result = service.listCoupons()

            assertThat(result).hasSize(2)
            assertThat(result[0].redeemedCount).isEqualTo(3L)
            assertThat(result[1].redeemedCount).isEqualTo(0L)
        }
    }
}
