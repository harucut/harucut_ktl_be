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

    @Test
    @DisplayName("maxOrUnlimited는 Limited면 max, Unlimited면 -1을 반환한다")
    fun maxOrUnlimited() {
        assertThat(Limit.Limited(5).maxOrUnlimited()).isEqualTo(5)
        assertThat(Limit.Unlimited.maxOrUnlimited()).isEqualTo(-1)
    }

    @Test
    @DisplayName("remainingFrom은 Limited면 (max-사용량, 0 하한), Unlimited면 -1을 반환한다")
    fun remainingFrom() {
        val limit = Limit.Limited(5)
        assertThat(limit.remainingFrom(2)).isEqualTo(3)
        assertThat(limit.remainingFrom(5)).isEqualTo(0)
        assertThat(limit.remainingFrom(7)).isEqualTo(0)
        assertThat(Limit.Unlimited.remainingFrom(999)).isEqualTo(-1)
    }
}