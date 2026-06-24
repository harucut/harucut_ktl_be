package com.harucut.subscription.usage

import com.harucut.frame.repository.FrameRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// 구독 사용량 계산 (요금제 한도 + 사이클/사용량). 만료된 사이클은 표시용으로만 0 계산하며 영속화하지 않음
@Service
@Transactional(readOnly = true)
class SubscriptionUsageService(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val frameRepository: FrameRepository
) {

    // 사용자의 구독 사용량 뷰를 계산해 반환
    fun getUsage(user: User): SubscriptionUsage {
        val subscription = resolveSubscription(user)
        val policy = subscription.planTier.policy
        val cycle = subscription.previewCycle(LocalDateTime.now())

        val videoLimit = policy.monthlyVideoUploadLimit
        val frameLimit = policy.frameRetentionLimit
        val frameUsed = frameRepository.countByUser(user).toInt()

        return SubscriptionUsage(
            planTier = subscription.planTier,
            videoUploadLimit = videoLimit.maxOrUnlimited(),
            videoUploadUsed = cycle.videoUploadCount,
            videoUploadRemaining = videoLimit.remainingFrom(cycle.videoUploadCount),
            videoUploadUnlimited = videoLimit.isUnlimited,
            frameRetentionLimit = frameLimit.maxOrUnlimited(),
            frameRetentionUsed = frameUsed,
            frameRetentionRemaining = frameLimit.remainingFrom(frameUsed),
            frameRetentionUnlimited = frameLimit.isUnlimited,
            cycleStart = cycle.start,
            cycleEnd = cycle.end
        )
    }

    // 영속 구독 조회. 없으면 비영속 기본 구독으로 계산 (저장하지 않음)
    private fun resolveSubscription(user: User): UserSubscription =
        userSubscriptionRepository.findByUserId(user.id!!) ?: UserSubscription.createDefault(user)
}
