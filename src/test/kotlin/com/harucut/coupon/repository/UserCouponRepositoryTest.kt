package com.harucut.coupon.repository

import com.harucut.coupon.entity.Coupon
import com.harucut.coupon.entity.UserCoupon
import com.harucut.subscription.plan.PlanTier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

@DataJpaTest
class UserCouponRepositoryTest {

    @Autowired
    lateinit var couponRepository: CouponRepository

    @Autowired
    lateinit var userCouponRepository: UserCouponRepository

    private fun coupon(code: String = "WELCOME-PRO"): Coupon =
        couponRepository.save(Coupon(name = "가입 축하 PRO 1개월", code = code, grantTier = PlanTier.PRO))

    @Test
    @DisplayName("저장/조회 시 매핑된 필드를 그대로 반환한다")
    fun mapping() {
        val c = coupon()
        val now = LocalDateTime.now()

        val saved = userCouponRepository.save(UserCoupon.redeemed(c, userId = 1L, now = now))

        val found = userCouponRepository.findById(saved.id!!).get()
        assertThat(found.userId).isEqualTo(1L)
        assertThat(found.coupon.id).isEqualTo(c.id)
        assertThat(found.redeemedAt).isEqualTo(now)
        assertThat(found.publicId).isNotBlank()
    }

    @Test
    @DisplayName("existsByUserIdAndCouponId는 사용자-쿠폰 조합 존재 여부를 반환한다")
    fun existsByUserIdAndCouponId() {
        val c = coupon()
        userCouponRepository.save(UserCoupon.redeemed(c, userId = 1L, now = LocalDateTime.now()))

        assertThat(userCouponRepository.existsByUserIdAndCouponId(1L, c.id!!)).isTrue()
        assertThat(userCouponRepository.existsByUserIdAndCouponId(2L, c.id!!)).isFalse()
    }

    @Test
    @DisplayName("countGroupedByCouponId는 쿠폰별 사용 수를 GROUP BY로 집계한다")
    fun countGroupedByCouponId() {
        val c1 = coupon()
        val c2 = coupon("OTHER-CODE")
        userCouponRepository.save(UserCoupon.redeemed(c1, userId = 1L, now = LocalDateTime.now()))
        userCouponRepository.save(UserCoupon.redeemed(c1, userId = 2L, now = LocalDateTime.now()))
        userCouponRepository.save(UserCoupon.redeemed(c2, userId = 1L, now = LocalDateTime.now()))

        val result = userCouponRepository.countGroupedByCouponId().associate { it.couponId to it.cnt }

        assertThat(result[c1.id]).isEqualTo(2L)
        assertThat(result[c2.id]).isEqualTo(1L)
    }

    @Test
    @DisplayName("deleteByUserId는 해당 사용자의 모든 쿠폰 사용 이력을 삭제한다")
    fun deleteByUserId() {
        val c = coupon()
        userCouponRepository.save(UserCoupon.redeemed(c, userId = 1L, now = LocalDateTime.now()))
        userCouponRepository.save(UserCoupon.redeemed(coupon("OTHER-CODE"), userId = 1L, now = LocalDateTime.now()))
        userCouponRepository.save(UserCoupon.redeemed(c, userId = 2L, now = LocalDateTime.now()))

        userCouponRepository.deleteByUserId(1L)

        assertThat(userCouponRepository.findAllByUserId(1L)).isEmpty()
        assertThat(userCouponRepository.findAllByUserId(2L)).hasSize(1)
    }
}
