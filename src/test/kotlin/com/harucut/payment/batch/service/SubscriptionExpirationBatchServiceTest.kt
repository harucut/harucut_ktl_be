package com.harucut.payment.batch.service

import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

class SubscriptionExpirationBatchServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val service = SubscriptionExpirationBatchService(userSubscriptionRepository)

    @Nested
    inner class ExpireInNewTransaction {

        @Test
        @DisplayName("구독이 존재하면 expireToFree()를 위임 호출한다")
        fun success() {
            val sub = mockk<UserSubscription>(relaxed = true)
            every { userSubscriptionRepository.findById(1L) } returns Optional.of(sub)

            service.expireInNewTransaction(1L)

            verify { sub.expireToFree() }
        }

        @Test
        @DisplayName("구독이 존재하지 않으면 아무 처리도 하지 않는다")
        fun notFound() {
            every { userSubscriptionRepository.findById(1L) } returns Optional.empty()

            service.expireInNewTransaction(1L)

            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }
    }
}
