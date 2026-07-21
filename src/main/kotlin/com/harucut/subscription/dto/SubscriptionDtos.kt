package com.harucut.subscription.dto

import com.harucut.subscription.entity.UserSubscription
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "구독 상태 응답")
data class SubscriptionResponse(
    @Schema(description = "요금제 단계", example = "PLUS")
    val planTier: String,
    @Schema(description = "구독 상태", example = "ACTIVE")
    val status: String,
    @Schema(description = "현재 결제 주기 시작", example = "2026-07-21T00:00:00")
    val currentPeriodStart: LocalDateTime?,
    @Schema(description = "현재 결제 주기 만료", example = "2026-08-21T00:00:00")
    val currentPeriodEnd: LocalDateTime?,
    @Schema(description = "자동갱신 여부", example = "true")
    val autoRenew: Boolean
) {
    companion object {
        fun from(subscription: UserSubscription) = SubscriptionResponse(
            planTier = subscription.planTier.name,
            status = subscription.status.name,
            currentPeriodStart = subscription.currentPeriodStart,
            currentPeriodEnd = subscription.currentPeriodEnd,
            autoRenew = subscription.autoRenew
        )
    }
}
