package com.harucut.subscription.plan

data class PlanPolicy(
    val monthlyVideoUploadLimit: Limit,
    val frameRetentionLimit: Limit,
    val historyRetention: Retention
)