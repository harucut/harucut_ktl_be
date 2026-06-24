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

    private fun subscription(tier: PlanTier, uploadCount: Int = 0): UserSubscription =
        mockk<UserSubscription>(relaxed = true).also {
            every { it.planTier } returns tier
            every { it.currentVideoUploadCount } returns uploadCount
        }

    @Nested
    inner class AssertAndConsumeVideoUploadQuota {

        @Test
        @DisplayName("한도 미만이면 사이클 동기화 후 업로드 횟수를 증가시킨다")
        fun underLimit() {
            val u = user()
            val sub = subscription(PlanTier.BASIC, uploadCount = 4)
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            service.assertAndConsumeVideoUploadQuota(u)

            verify { sub.syncQuotaCycle(any()) }
            verify { sub.increaseVideoUploadCount() }
        }

        @Test
        @DisplayName("BASIC 월 한도(5)에 도달하면 PLAN_VIDEO_UPLOAD_LIMIT_EXCEEDED 예외를 던진다")
        fun atLimit() {
            val u = user()
            val sub = subscription(PlanTier.BASIC, uploadCount = 5)
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            assertThatThrownBy { service.assertAndConsumeVideoUploadQuota(u) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_VIDEO_UPLOAD_LIMIT_EXCEEDED)

            verify(exactly = 0) { sub.increaseVideoUploadCount() }
        }

        @Test
        @DisplayName("PRO(무제한)는 사용량과 무관하게 통과하고 횟수를 증가시킨다")
        fun unlimited() {
            val u = user()
            val sub = subscription(PlanTier.PRO, uploadCount = 9999)
            every { userSubscriptionRepository.findByUserId(1L) } returns sub

            service.assertAndConsumeVideoUploadQuota(u)

            verify { sub.increaseVideoUploadCount() }
        }

        @Test
        @DisplayName("구독이 없으면 기본 구독을 생성·저장한 뒤 차감한다")
        fun lazyCreate() {
            val u = user()
            val saved = subscription(PlanTier.BASIC, uploadCount = 0)
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { userSubscriptionRepository.save(any<UserSubscription>()) } returns saved

            service.assertAndConsumeVideoUploadQuota(u)

            verify { userSubscriptionRepository.save(any<UserSubscription>()) }
            verify { saved.increaseVideoUploadCount() }
        }
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
        @DisplayName("BASIC 동시 보관 cap(1) 미만이면 통과한다")
        fun underCap() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            service.assertFrameRetentionLimit(u, 0)
        }

        @Test
        @DisplayName("BASIC 동시 보관 cap(1)에 도달하면 PLAN_FRAME_RETENTION_EXCEEDED 예외를 던진다")
        fun atCap() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.BASIC)

            assertThatThrownBy { service.assertFrameRetentionLimit(u, 1) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }

        @Test
        @DisplayName("PRO도 동시 보관 cap(10)을 초과하면 예외를 던진다")
        fun proAtCap() {
            val u = user()
            every { userSubscriptionRepository.findByUserId(1L) } returns subscription(PlanTier.PRO)

            assertThatThrownBy { service.assertFrameRetentionLimit(u, 10) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
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

}