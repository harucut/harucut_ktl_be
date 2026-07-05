package com.harucut.user.config

import com.harucut.subscription.plan.PlanTier
import org.springframework.boot.context.properties.ConfigurationProperties

// 요금제별 월 구독료(원) — billing.pricing.* 로 오버라이드 가능 (기본: BASIC 0 / PLUS 3900 / PRO 9900)
@ConfigurationProperties(prefix = "billing.pricing")
data class PlanPricingProperties(
    val basic: Int = 0,
    val plus: Int = 3900,
    val pro: Int = 9900
) {
    // 요금제 단계에 해당하는 월 구독료 반환
    fun priceOf(tier: PlanTier): Int = when (tier) {
        PlanTier.BASIC -> basic
        PlanTier.PLUS -> plus
        PlanTier.PRO -> pro
    }
}
