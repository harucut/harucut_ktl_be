package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SubscriptionPolicyServiceTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val service = SubscriptionPolicyService(userSubscriptionRepository)

    private fun user(id: Long? = 1L): User = mockk<User>(relaxed = true).also { every { it.id } returns id }

    private fun subscription(tier: PlanTier): UserSubscription =
        mockk<UserSubscription>(relaxed = true).also {
            every { it.planTier } returns tier
        }

    @Nested
    inner class ResolveHistoryCutoff {

        @Test
        @DisplayName("BASIC은 현재 시각 기준 보관 cutoff를 반환한다")
        fun basic() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            val cutoff = service.resolveHistoryCutoff(u)

            assertThat(cutoff).isNotNull()
            assertThat(cutoff).isBefore(LocalDateTime.now())
        }

        @Test
        @DisplayName("PRO(무제한)는 cutoff가 null이다")
        fun unlimited() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PRO)

            assertThat(service.resolveHistoryCutoff(u)).isNull()
        }

        @Test
        @DisplayName("구독이 없으면 기본 요금제로 판정하며 저장하지 않는다")
        fun noSubscription() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns null

            val cutoff = service.resolveHistoryCutoff(u)

            assertThat(cutoff).isNotNull()
            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }
    }

    @Nested
    inner class AssertHistoryAccessible {

        @Test
        @DisplayName("BASIC에서 보관 기간 이내면 통과한다")
        fun within() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            service.assertHistoryAccessible(u, LocalDateTime.now().minusDays(1))
        }

        @Test
        @DisplayName("BASIC에서 보관 기간을 초과하면 PLAN_HISTORY_RETENTION_EXCEEDED 예외를 던진다")
        fun beyond() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            assertThatThrownBy { service.assertHistoryAccessible(u, LocalDateTime.now().minusDays(10)) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED)
        }

        @Test
        @DisplayName("createdAt이 null이면 항상 통과한다")
        fun nullCreatedAt() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            service.assertHistoryAccessible(u, null)
        }

        @Test
        @DisplayName("무제한 요금제는 기간과 무관하게 통과한다")
        fun unlimitedAccess() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PRO)

            service.assertHistoryAccessible(u, LocalDateTime.now().minusYears(5))
        }
    }

    @Nested
    inner class AssertFrameRetentionLimit {

        @Test
        @DisplayName("PLUS 동시 보관 cap(3) 미만이면 통과한다")
        fun underCap() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PLUS)

            service.assertFrameRetentionLimit(u, 2)
        }

        @Test
        @DisplayName("PLUS 동시 보관 cap(3)에 도달하면 PLAN_FRAME_RETENTION_EXCEEDED 예외를 던진다")
        fun atCap() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PLUS)

            assertThatThrownBy { service.assertFrameRetentionLimit(u, 3) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }

        @Test
        @DisplayName("BASIC은 cap(0)이라 보관 프레임이 없어도 예외를 던진다")
        fun basicAlwaysBlocked() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            assertThatThrownBy { service.assertFrameRetentionLimit(u, 0) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }

        @Test
        @DisplayName("PRO(무제한)는 개수와 무관하게 통과한다")
        fun proUnlimited() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PRO)

            service.assertFrameRetentionLimit(u, 100)
        }

        @Test
        @DisplayName("구독이 없으면 기본 요금제 cap으로 판정하며 저장하지 않는다")
        fun noSubscription() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns null

            assertThatThrownBy { service.assertFrameRetentionLimit(u, 1) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)

            verify(exactly = 0) { userSubscriptionRepository.save(any()) }
        }
    }

    @Nested
    inner class ResolveFrameRetentionCap {

        @Test
        @DisplayName("유한 cap 요금제는 cap 개수를 반환한다")
        fun limited() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PLUS)

            assertThat(service.resolveFrameRetentionCap(u)).isEqualTo(3)
        }

        @Test
        @DisplayName("BASIC은 cap 0을 반환한다")
        fun basicZero() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            assertThat(service.resolveFrameRetentionCap(u)).isEqualTo(0)
        }

        @Test
        @DisplayName("무제한 요금제는 null을 반환한다")
        fun unlimited() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PRO)

            assertThat(service.resolveFrameRetentionCap(u)).isNull()
        }
    }

}
