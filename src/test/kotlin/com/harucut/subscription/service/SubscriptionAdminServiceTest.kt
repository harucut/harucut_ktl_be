package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SubscriptionAdminServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val service = SubscriptionAdminServiceImpl(userSubscriptionRepository)

    private fun subscription(): UserSubscription = mockk(relaxed = true)

    @Nested
    inner class GetSubscription {

        @Test
        @DisplayName("구독이 없으면 NO_ACTIVE_SUBSCRIPTION(SUBS-004) 예외를 던진다")
        fun notFound() {
            every { userSubscriptionRepository.findByUserId(1L) } returns null

            assertThatThrownBy { service.getSubscription(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)
        }
    }

    @Nested
    inner class ChangePlan {

        @Test
        @DisplayName("관리자가 요금제를 강제 변경한다")
        fun success() {
            val sub = subscription()
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            service.changePlan(1L, PlanTier.PRO)

            verify { sub.changePlan(PlanTier.PRO) }
        }

        @Test
        @DisplayName("구독이 없으면 NO_ACTIVE_SUBSCRIPTION(SUBS-004) 예외를 던진다")
        fun notFound() {
            every { userSubscriptionRepository.findByUserId(1L) } returns null

            assertThatThrownBy { service.changePlan(1L, PlanTier.PRO) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)
        }
    }
}
