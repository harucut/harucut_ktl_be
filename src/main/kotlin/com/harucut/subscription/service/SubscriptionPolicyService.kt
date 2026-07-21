package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.frame.policy.FrameSubscriptionPolicy
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.plan.PlanPolicy
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class SubscriptionPolicyService(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val clock: Clock
) : MediaSubscriptionPolicy, FrameSubscriptionPolicy {

    override fun resolveHistoryCutoff(user: User): LocalDateTime? =
        resolvePolicy(user).historyRetention.cutoffFrom(LocalDateTime.now())

    override fun assertHistoryAccessible(user: User, createdAt: LocalDateTime?) {
        val retention = resolvePolicy(user).historyRetention
        if (!retention.isAccessible(createdAt, LocalDateTime.now())) {
            throw BusinessException(SubscriptionErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED)
        }
    }

    override fun assertFrameRetentionLimit(user: User, currentFrameCount: Int) {
        val limit = resolvePolicy(user).frameRetentionLimit
        if (!limit.allows(currentFrameCount)) {
            throw BusinessException(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }
    }

    override fun resolveFrameRetentionCap(user: User): Int? {
        val limit = resolvePolicy(user).frameRetentionLimit
        return if (limit.isUnlimited) null else limit.maxOrUnlimited()
    }

    private fun resolvePolicy(user: User): PlanPolicy {
        val userId = user.id ?: return PlanTier.DEFAULT.policy
        val subscription = userSubscriptionRepository.findByUserId(userId) ?: return PlanTier.DEFAULT.policy
        return subscription.effectiveTier(LocalDateTime.now(clock)).policy
    }
}
