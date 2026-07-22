package com.harucut.subscription.usage

import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class SubscriptionUsageService(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val frameRepository: FrameRepository,
    private val clock: Clock
) {

    fun getUsage(user: User): SubscriptionUsage {
        val subscription = resolveSubscription(user)
        val effectiveTier = subscription.effectiveTier(LocalDateTime.now(clock))
        val policy = effectiveTier.policy

        val frameLimit = policy.frameRetentionLimit
        val frameUsed = frameRepository.countByUser(user).toInt()

        return SubscriptionUsage(
            planTier = effectiveTier,
            frameRetentionLimit = frameLimit.maxOrUnlimited(),
            frameRetentionUsed = frameUsed,
            frameRetentionRemaining = frameLimit.remainingFrom(frameUsed),
            frameRetentionUnlimited = frameLimit.isUnlimited
        )
    }

    private fun resolveSubscription(user: User): UserSubscription =
        userSubscriptionRepository.findByUserId(user.id!!) ?: UserSubscription.createDefault(user)
}
