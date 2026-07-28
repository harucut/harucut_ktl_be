package com.harucut.payment.batch.service

import com.harucut.coupon.service.GrantActivationService
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

class SubscriptionExpirationBatchServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val grantActivationService = mockk<GrantActivationService>()
    private val service = SubscriptionExpirationBatchService(userSubscriptionRepository, grantActivationService)
    private val now = LocalDateTime.now()

    @Nested
    inner class ExpireInNewTransaction {

        @Test
        @DisplayName("예약된 grant가 없으면 expireToFree()를 위임 호출한다")
        fun success() {
            val sub = mockk<UserSubscription>(relaxed = true)
            every { sub.reservedGrantCouponId } returns null
            every { userSubscriptionRepository.findById(1L) } returns Optional.of(sub)

            service.expireInNewTransaction(1L, now)

            verify { sub.expireToFree() }
            verify(exactly = 0) { grantActivationService.activate(any(), any()) }
        }

        @Test
        @DisplayName("예약된 grant가 있으면 강등 대신 grant를 활성화한다")
        fun reservedGrant() {
            val sub = mockk<UserSubscription>(relaxed = true)
            every { sub.reservedGrantCouponId } returns 10L
            every { userSubscriptionRepository.findById(1L) } returns Optional.of(sub)
            every { grantActivationService.activate(sub, now) } just runs

            service.expireInNewTransaction(1L, now)

            verify { grantActivationService.activate(sub, now) }
            verify(exactly = 0) { sub.expireToFree() }
        }

        @Test
        @DisplayName("구독이 존재하지 않으면 아무 처리도 하지 않는다")
        fun notFound() {
            every { userSubscriptionRepository.findById(1L) } returns Optional.empty()

            service.expireInNewTransaction(1L, now)

            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }
    }
}
