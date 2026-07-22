package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SubscriptionServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val service = SubscriptionServiceImpl(userSubscriptionRepository)

    private fun subscription(status: SubscriptionStatus = SubscriptionStatus.ACTIVE): UserSubscription =
        mockk(relaxed = true) { every { this@mockk.status } returns status }

    @Nested
    inner class GetMySubscription {

        @Test
        @DisplayName("구독이 없으면 NO_ACTIVE_SUBSCRIPTION(SUBS-004) 예외를 던진다")
        fun notFound() {
            every { userSubscriptionRepository.findByUserId(1L) } returns null

            assertThatThrownBy { service.getMySubscription(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)
        }
    }

    @Nested
    inner class CancelAutoRenew {

        @Test
        @DisplayName("정상 해지를 요청하면 구독의 cancelAutoRenew를 호출한다")
        fun success() {
            val sub = subscription(SubscriptionStatus.ACTIVE)
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            service.cancelAutoRenew(1L)

            verify { sub.cancelAutoRenew() }
        }

        @Test
        @DisplayName("이미 해지된 구독이면 ALREADY_CANCELED(SUBS-005) 예외를 던진다")
        fun alreadyCanceled() {
            val sub = subscription(SubscriptionStatus.CANCELED)
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            assertThatThrownBy { service.cancelAutoRenew(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.ALREADY_CANCELED)

            verify(exactly = 0) { sub.cancelAutoRenew() }
        }

        @Test
        @DisplayName("구독이 없으면 NO_ACTIVE_SUBSCRIPTION(SUBS-004) 예외를 던진다")
        fun notFound() {
            every { userSubscriptionRepository.findByUserId(1L) } returns null

            assertThatThrownBy { service.cancelAutoRenew(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)
        }
    }
}
