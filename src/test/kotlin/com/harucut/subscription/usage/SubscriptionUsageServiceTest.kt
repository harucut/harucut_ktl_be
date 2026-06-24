package com.harucut.subscription.usage

import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.entity.CycleView
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
import java.time.LocalDateTime

class SubscriptionUsageServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val frameRepository = mockk<FrameRepository>()

    private val service = SubscriptionUsageService(userSubscriptionRepository, frameRepository)

    private fun userMock(id: Long = 1L): User = mockk(relaxed = true) {
        every { this@mockk.id } returns id
    }

    // planTier와 사이클 뷰를 스텁한 구독 목 (previewCycle 자체 로직은 UserSubscriptionTest가 검증)
    private fun subMock(tier: PlanTier, cycle: CycleView): UserSubscription = mockk(relaxed = true) {
        every { planTier } returns tier
        every { previewCycle(any()) } returns cycle
    }

    private fun cycle(videoUsed: Int): CycleView = CycleView(
        LocalDateTime.now().minusDays(10),
        LocalDateTime.now().plusDays(20),
        videoUsed
    )

    @Nested
    inner class GetUsage {

        @Test
        @DisplayName("BASIC: 동영상 업로드/프레임 보관 한도·사용량·잔여를 계산한다")
        fun basic() {
            val user = userMock()
            every { userSubscriptionRepository.findByUserId(1L) } returns subMock(PlanTier.BASIC, cycle(2))
            every { frameRepository.countByUser(user) } returns 1L

            val usage = service.getUsage(user)

            assertThat(usage.planTier).isEqualTo(PlanTier.BASIC)
            assertThat(usage.videoUploadLimit).isEqualTo(5)
            assertThat(usage.videoUploadUsed).isEqualTo(2)
            assertThat(usage.videoUploadRemaining).isEqualTo(3)
            assertThat(usage.videoUploadUnlimited).isFalse()
            assertThat(usage.frameRetentionLimit).isEqualTo(1)
            assertThat(usage.frameRetentionUsed).isEqualTo(1)
            assertThat(usage.frameRetentionRemaining).isEqualTo(0)
            assertThat(usage.frameRetentionUnlimited).isFalse()
        }

        @Test
        @DisplayName("PRO: 동영상 업로드 무제한은 한도·잔여 -1, 무제한 플래그 true이다")
        fun proUnlimitedVideo() {
            val user = userMock()
            every { userSubscriptionRepository.findByUserId(1L) } returns subMock(PlanTier.PRO, cycle(999))
            every { frameRepository.countByUser(user) } returns 3L

            val usage = service.getUsage(user)

            assertThat(usage.videoUploadLimit).isEqualTo(-1)
            assertThat(usage.videoUploadRemaining).isEqualTo(-1)
            assertThat(usage.videoUploadUnlimited).isTrue()
            assertThat(usage.frameRetentionLimit).isEqualTo(10)
            assertThat(usage.frameRetentionRemaining).isEqualTo(7)
        }

        @Test
        @DisplayName("만료 사이클(뷰의 사용량 0)을 그대로 반영하고 구독을 저장/동기화하지 않는다")
        fun expiredReadonly() {
            val user = userMock()
            val sub = subMock(PlanTier.BASIC, CycleView(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().plusDays(25),
                0
            ))
            every { userSubscriptionRepository.findByUserId(1L) } returns sub
            every { frameRepository.countByUser(user) } returns 0L

            val usage = service.getUsage(user)

            assertThat(usage.videoUploadUsed).isEqualTo(0)
            assertThat(usage.cycleEnd).isAfter(LocalDateTime.now())
            verify(exactly = 0) { sub.syncQuotaCycle(any()) }
            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }

        @Test
        @DisplayName("구독이 없으면 기본 구독(BASIC, 사용량 0)으로 계산하고 저장하지 않는다")
        fun noSubscription() {
            val user = userMock()
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { frameRepository.countByUser(user) } returns 0L

            val usage = service.getUsage(user)

            assertThat(usage.planTier).isEqualTo(PlanTier.BASIC)
            assertThat(usage.videoUploadUsed).isEqualTo(0)
            assertThat(usage.frameRetentionUsed).isEqualTo(0)
            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }
    }
}
