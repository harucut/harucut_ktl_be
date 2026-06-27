package com.harucut.subscription.plan

data class PlanPolicy(
    val frameRetentionLimit: Limit,
    val historyRetention: Retention
)