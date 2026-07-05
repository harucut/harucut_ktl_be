package com.harucut.user.config

import com.harucut.subscription.plan.PlanTier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlanPricingPropertiesTest {

    private val properties = PlanPricingProperties()

    @Test
    @DisplayName("기본 요금제별 월 구독료를 반환한다 (BASIC 0 / PLUS 3900 / PRO 9900)")
    fun defaultPrices() {
        assertThat(properties.priceOf(PlanTier.BASIC)).isEqualTo(0)
        assertThat(properties.priceOf(PlanTier.PLUS)).isEqualTo(3900)
        assertThat(properties.priceOf(PlanTier.PRO)).isEqualTo(9900)
    }

    @Test
    @DisplayName("오버라이드된 가격을 반영한다")
    fun overriddenPrices() {
        val custom = PlanPricingProperties(basic = 100, plus = 5000, pro = 20000)

        assertThat(custom.priceOf(PlanTier.PLUS)).isEqualTo(5000)
        assertThat(custom.priceOf(PlanTier.PRO)).isEqualTo(20000)
    }
}
