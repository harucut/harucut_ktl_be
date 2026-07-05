package com.harucut.subscription.plan

enum class PlanTier(val policy: PlanPolicy) {

    BASIC(
        PlanPolicy(
            frameRetentionLimit = Limit.Limited(0),
            historyRetention = Retention.Days(3)
        )
    ),
    PLUS(
        PlanPolicy(
            frameRetentionLimit = Limit.Limited(3),
            historyRetention = Retention.Months(3)
        )
    ),
    PRO(
        PlanPolicy(
            frameRetentionLimit = Limit.Unlimited,
            historyRetention = Retention.Unlimited
        )
    );

    companion object {
        val DEFAULT: PlanTier = BASIC
    }
}
