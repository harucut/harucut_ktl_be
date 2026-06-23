package com.harucut.subscription.plan

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LimitTest {

    @Test
    @DisplayName("Limited는 현재 사용량이 max 미만일 때만 허용한다")
    fun limited() {
        val limit = Limit.Limited(5)

        assertThat(limit.allows(4)).isTrue()
        assertThat(limit.allows(5)).isFalse()
        assertThat(limit.isUnlimited).isFalse()
    }

    @Test
    @DisplayName("Unlimited는 항상 허용한다")
    fun unlimited() {
        assertThat(Limit.Unlimited.allows(Int.MAX_VALUE)).isTrue()
        assertThat(Limit.Unlimited.isUnlimited).isTrue()
    }
}