package com.harucut.subscription.usage

import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SubscriptionUsageServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val frameRepository = mockk<FrameRepository>()

    private val service = SubscriptionUsageService(userSubscriptionRepository, frameRepository)

    private fun userMock(id: Long = 1L): User = mockk(relaxed = true) {
        every { this@mockk.id } returns id
    }

    private fun subMock(tier: PlanTier): UserSubscription = mockk(relaxed = true) {
        every { planTier } returns tier
    }

    @Nested
    inner class GetUsage {

        @Test
        @DisplayName("BASIC: 프레임 보관 한도·사용량·잔여를 계산한다")
        fun basic() {
            val user = userMock()
            every { userSubscriptionRepository.findByUserId(1L) } returns subMock(PlanTier.BASIC)
            every { frameRepository.countByUser(user) } returns 1L

            val usage = service.getUsage(user)

            assertThat(usage.planTier).isEqualTo(PlanTier.BASIC)
            assertThat(usage.frameRetentionLimit).isEqualTo(1)
            assertThat(usage.frameRetentionUsed).isEqualTo(1)
            assertThat(usage.frameRetentionRemaining).isEqualTo(0)
            assertThat(usage.frameRetentionUnlimited).isFalse()
        }

        @Test
        @DisplayName("PRO: 프레임 보관 cap(10)과 사용량을 정확히 계산한다")
        fun pro() {
            val user = userMock()
            every { userSubscriptionRepository.findByUserId(1L) } returns subMock(PlanTier.PRO)
            every { frameRepository.countByUser(user) } returns 3L

            val usage = service.getUsage(user)

            assertThat(usage.frameRetentionLimit).isEqualTo(10)
            assertThat(usage.frameRetentionRemaining).isEqualTo(7)
        }

        @Test
        @DisplayName("구독이 없으면 기본 구독(BASIC, 사용량 0)으로 계산하고 저장하지 않는다")
        fun noSubscription() {
            val user = userMock()
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { frameRepository.countByUser(user) } returns 0L

            val usage = service.getUsage(user)

            assertThat(usage.planTier).isEqualTo(PlanTier.BASIC)
            assertThat(usage.frameRetentionUsed).isEqualTo(0)
            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }
    }
}
