package com.harucut.subscription.usage

import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SubscriptionUsageService(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val frameRepository: FrameRepository
) {

    fun getUsage(user: User): SubscriptionUsage {
        val subscription = resolveSubscription(user)
        val policy = subscription.planTier.policy

        val frameLimit = policy.frameRetentionLimit
        val frameUsed = frameRepository.countByUser(user).toInt()

        return SubscriptionUsage(
            planTier = subscription.planTier,
            frameRetentionLimit = frameLimit.maxOrUnlimited(),
            frameRetentionUsed = frameUsed,
            frameRetentionRemaining = frameLimit.remainingFrom(frameUsed),
            frameRetentionUnlimited = frameLimit.isUnlimited
        )
    }

    private fun resolveSubscription(user: User): UserSubscription =
        userSubscriptionRepository.findByUserId(user.id!!) ?: UserSubscription.createDefault(user)
}
