package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.frame.policy.FrameSubscriptionPolicy
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.plan.PlanPolicy
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SubscriptionPolicyService(
    private val userSubscriptionRepository: UserSubscriptionRepository
) : MediaSubscriptionPolicy, FrameSubscriptionPolicy {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 요금제 보관 정책의 cutoff 시각. 무제한이면 null */
    override fun resolveHistoryCutoff(user: User): LocalDateTime? =
        resolvePolicy(user).historyRetention.cutoffFrom(LocalDateTime.now())

    /** createdAt이 요금제 보관 기간 내인지 검증 */
    override fun assertHistoryAccessible(user: User, createdAt: LocalDateTime?) {
        val retention = resolvePolicy(user).historyRetention
        if (!retention.isAccessible(createdAt, LocalDateTime.now())) {
            throw BusinessException(SubscriptionErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED)
        }
    }

    /** 동영상 업로드 월 쿼터 검증 후 사용량 차감 */
    override fun assertAndConsumeVideoUploadQuota(user: User) {
        val subscription = resolveForConsume(user)
        subscription.syncQuotaCycle(LocalDateTime.now())

        val limit = subscription.planTier.policy.monthlyVideoUploadLimit
        if (!limit.allows(subscription.currentVideoUploadCount)) {
            throw BusinessException(SubscriptionErrorCode.PLAN_VIDEO_UPLOAD_LIMIT_EXCEEDED)
        }

        subscription.increaseVideoUploadCount()
    }


    /** 읽기 판정용 요금제 정책. 구독이 없으면 기본 요금제로 (비영속) */
    private fun resolvePolicy(user: User): PlanPolicy {
        val userId = user.id ?: return PlanTier.DEFAULT.policy
        return (userSubscriptionRepository.findByUserId(userId)?.planTier ?: PlanTier.DEFAULT).policy
    }

    /** 쿼터 차감용 구독 조회. 없으면 기본 구독을 생성·영속화 */
    private fun resolveForConsume(user: User): UserSubscription =
        userSubscriptionRepository.findByUserId(user.id!!)
            ?: run {
                log.warn("구독이 없어 기본 구독을 생성합니다. userId={}", user.id)
                userSubscriptionRepository.save(UserSubscription.createDefault(user))
            }

    /** 현재 보관 중인 프레임 개수가 요금제 동시 보관 cap 이내인지 검증 (초과 시 예외) */
    override fun assertFrameRetentionLimit(user: User, currentFrameCount: Int) {
        val limit = resolvePolicy(user).frameRetentionLimit
        if (!limit.allows(currentFrameCount)) {
            throw BusinessException(SubscriptionErrorCode.PLAN_FRAME_RETENTION_EXCEEDED)
        }
    }
}