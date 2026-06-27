package com.harucut.subscription.plan

enum class PlanTier(val policy: PlanPolicy) {

    BASIC(
        PlanPolicy(
            frameRetentionLimit = Limit.Limited(1),
            historyRetention = Retention.Days(3)
        )
    ),
    PLUS(
        PlanPolicy(
            frameRetentionLimit = Limit.Limited(5),
            historyRetention = Retention.Unlimited
        )
    ),
    PRO(
        PlanPolicy(
            frameRetentionLimit = Limit.Limited(10),
            historyRetention = Retention.Unlimited
        )
    );

    companion object {
        val DEFAULT: PlanTier = BASIC
    }
}
