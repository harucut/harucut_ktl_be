package com.harucut.coupon.service

import com.harucut.coupon.entity.Coupon
import com.harucut.coupon.entity.UserCoupon
import com.harucut.coupon.enums.UserCouponStatus
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.repository.CouponRepository
import com.harucut.coupon.repository.UserCouponRepository
import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CouponServiceImplTest {

    private val couponRepository = mockk<CouponRepository>()
    private val userCouponRepository = mockk<UserCouponRepository>()
    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val userRepository = mockk<UserRepository>()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)
    private val now = LocalDateTime.now(fixedClock)

    private val service = CouponServiceImpl(
        couponRepository, userCouponRepository, userSubscriptionRepository, userRepository, fixedClock
    )

    private fun coupon(
        id: Long = 1L,
        tier: PlanTier = PlanTier.PRO,
        maxRedemptions: Int? = null,
        redeemable: Boolean = true
    ): Coupon = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { grantTier } returns tier
        every { this@mockk.maxRedemptions } returns maxRedemptions
        every { isRedeemable(any()) } returns redeemable
    }

    private fun subscription(
        effectiveTier: PlanTier = PlanTier.BASIC,
        reservedGrantCouponId: Long? = null,
        currentPeriodEnd: LocalDateTime? = null
    ): UserSubscription = mockk(relaxed = true) {
        every { effectiveTier(any()) } returns effectiveTier
        every { this@mockk.reservedGrantCouponId } returns reservedGrantCouponId
        every { this@mockk.currentPeriodEnd } returns currentPeriodEnd
    }

    @Nested
    inner class Redeem {

        @Test
        @DisplayName("무료 구독(BASIC)이면 즉시 grant를 개시하고 쿠폰을 REDEEMED로 저장한다")
        fun immediateActivation() {
            val c = coupon()
            val sub = subscription(effectiveTier = PlanTier.BASIC)
            every { couponRepository.findByCode("WELCOME-PRO") } returns c
            every { userCouponRepository.existsByUserIdAndCouponId(1L, 1L) } returns false
            every { userSubscriptionRepository.findByUserId(1L) } returns sub
            val slot = slot<UserCoupon>()
            every { userCouponRepository.save(capture(slot)) } answers { slot.captured }

            val result = service.redeem(1L, "WELCOME-PRO")

            verify { sub.activateGrant(PlanTier.PRO, now, now.plusMonths(1)) }
            assertThat(slot.captured.status).isEqualTo(UserCouponStatus.REDEEMED)
            assertThat(result.applied).isTrue()
            assertThat(result.grantTier).isEqualTo(PlanTier.PRO)
            assertThat(result.startsAt).isEqualTo(now)
            assertThat(result.endsAt).isEqualTo(now.plusMonths(1))
        }

        @Test
        @DisplayName("tier 접근 중이면 현 주기 후로 예약한다")
        fun reserve() {
            val c = coupon()
            val periodEnd = now.plusDays(10)
            val sub = subscription(effectiveTier = PlanTier.PLUS, currentPeriodEnd = periodEnd)
            every { couponRepository.findByCode("WELCOME-PRO") } returns c
            every { userCouponRepository.existsByUserIdAndCouponId(1L, 1L) } returns false
            every { userSubscriptionRepository.findByUserId(1L) } returns sub
            val savedUserCoupon = mockk<UserCoupon>(relaxed = true) { every { id } returns 99L }
            every { userCouponRepository.save(any()) } returns savedUserCoupon

            val result = service.redeem(1L, "WELCOME-PRO")

            verify { sub.reserveGrant(99L) }
            verify(exactly = 0) { sub.activateGrant(any(), any(), any()) }
            assertThat(result.applied).isFalse()
            assertThat(result.startsAt).isEqualTo(periodEnd)
            assertThat(result.endsAt).isEqualTo(periodEnd.plusMonths(1))
        }

        @Test
        @DisplayName("구독이 없으면 기본(BASIC) 구독을 생성하고 즉시 개시한다")
        fun createsDefaultSubscriptionWhenMissing() {
            val c = coupon()
            val user = mockk<User>(relaxed = true)
            val createdSub = subscription(effectiveTier = PlanTier.BASIC)
            every { couponRepository.findByCode("WELCOME-PRO") } returns c
            every { userCouponRepository.existsByUserIdAndCouponId(1L, 1L) } returns false
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { userRepository.getReferenceById(1L) } returns user
            every { userSubscriptionRepository.save(any()) } returns createdSub
            every { userCouponRepository.save(any()) } returns mockk(relaxed = true)

            val result = service.redeem(1L, "WELCOME-PRO")

            verify { userSubscriptionRepository.save(any()) }
            assertThat(result.applied).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 코드면 COUPON_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { couponRepository.findByCode("NO-CODE") } returns null

            assertThatThrownBy { service.redeem(1L, "NO-CODE") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_NOT_FOUND)
        }

        @Test
        @DisplayName("비활성/사용마감 쿠폰이면 COUPON_INACTIVE 예외를 던진다")
        fun inactive() {
            every { couponRepository.findByCode("WELCOME-PRO") } returns coupon(redeemable = false)

            assertThatThrownBy { service.redeem(1L, "WELCOME-PRO") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_INACTIVE)
        }

        @Test
        @DisplayName("사용 상한에 도달했으면 COUPON_EXHAUSTED 예외를 던진다")
        fun exhausted() {
            every { couponRepository.findByCode("WELCOME-PRO") } returns coupon(maxRedemptions = 5)
            every { userCouponRepository.countByCouponId(1L) } returns 5L

            assertThatThrownBy { service.redeem(1L, "WELCOME-PRO") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_EXHAUSTED)
        }

        @Test
        @DisplayName("이미 사용한 쿠폰이면 COUPON_ALREADY_REDEEMED 예외를 던진다")
        fun alreadyRedeemed() {
            every { couponRepository.findByCode("WELCOME-PRO") } returns coupon()
            every { userCouponRepository.existsByUserIdAndCouponId(1L, 1L) } returns true

            assertThatThrownBy { service.redeem(1L, "WELCOME-PRO") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.COUPON_ALREADY_REDEEMED)
        }

        @Test
        @DisplayName("이미 예약된 쿠폰이 있으면 RESERVATION_EXISTS 예외를 던진다")
        fun reservationExists() {
            val sub = subscription(effectiveTier = PlanTier.PLUS, reservedGrantCouponId = 50L)
            every { couponRepository.findByCode("WELCOME-PRO") } returns coupon()
            every { userCouponRepository.existsByUserIdAndCouponId(1L, 1L) } returns false
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            assertThatThrownBy { service.redeem(1L, "WELCOME-PRO") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.RESERVATION_EXISTS)

            verify(exactly = 0) { userCouponRepository.save(any()) }
        }

        @Test
        @DisplayName("BASIC이어도 기존 예약이 있으면 RESERVATION_EXISTS 예외를 던진다")
        fun reservationExistsEvenWhenBasic() {
            val sub = subscription(effectiveTier = PlanTier.BASIC, reservedGrantCouponId = 50L)
            every { couponRepository.findByCode("WELCOME-PRO") } returns coupon()
            every { userCouponRepository.existsByUserIdAndCouponId(1L, 1L) } returns false
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            assertThatThrownBy { service.redeem(1L, "WELCOME-PRO") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CouponErrorCode.RESERVATION_EXISTS)

            verify(exactly = 0) { userCouponRepository.save(any()) }
            verify(exactly = 0) { sub.activateGrant(any(), any(), any()) }
        }
    }

    @Nested
    inner class GetMyCoupons {

        @Test
        @DisplayName("사용자의 쿠폰 사용 이력을 응답으로 변환해 반환한다")
        fun success() {
            val c = coupon()
            every { c.name } returns "가입 축하 PRO 1개월"
            val uc = UserCoupon.redeemed(c, userId = 1L, now = now)
            every { userCouponRepository.findAllByUserId(1L) } returns listOf(uc)

            val result = service.getMyCoupons(1L)

            assertThat(result).hasSize(1)
            assertThat(result[0].couponName).isEqualTo("가입 축하 PRO 1개월")
            assertThat(result[0].status).isEqualTo(UserCouponStatus.REDEEMED)
        }
    }
}
