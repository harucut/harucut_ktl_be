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
import java.time.LocalDateTime

@Service
class SubscriptionPolicyService(
    private val userSubscriptionRepository: UserSubscriptionRepository
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

    private fun resolvePolicy(user: User): PlanPolicy {
        val userId = user.id ?: return PlanTier.DEFAULT.policy
        return (userSubscriptionRepository.findByUserId(userId)?.planTier ?: PlanTier.DEFAULT).policy
    }
}
