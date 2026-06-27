package com.harucut.subscription.usage

import com.harucut.subscription.plan.PlanTier

data class SubscriptionUsage(
    val planTier: PlanTier,

    val frameRetentionLimit: Int,
    val frameRetentionUsed: Int,
    val frameRetentionRemaining: Int,
    val frameRetentionUnlimited: Boolean
)
