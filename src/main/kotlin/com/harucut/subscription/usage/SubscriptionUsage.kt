package com.harucut.subscription.usage

import com.harucut.subscription.plan.PlanTier
import java.time.LocalDateTime

// 구독 사용량 도메인 뷰 (동영상 업로드 월 사용량 + 프레임 동시 보관 사용량). 한도/잔여 -1 = 무제한
data class SubscriptionUsage(
    val planTier: PlanTier,

    val videoUploadLimit: Int,
    val videoUploadUsed: Int,
    val videoUploadRemaining: Int,
    val videoUploadUnlimited: Boolean,

    val frameRetentionLimit: Int,
    val frameRetentionUsed: Int,
    val frameRetentionRemaining: Int,
    val frameRetentionUnlimited: Boolean,

    val cycleStart: LocalDateTime,
    val cycleEnd: LocalDateTime
)
