package com.harucut.subscription.plan

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RetentionTest {

    private val now = LocalDateTime.of(2026, 6, 21, 0, 0)

    @Test
    @DisplayName("Days는 now 기준 N일 전 cutoff를 만들고 그 이후 생성물만 접근 가능하다")
    fun days() {
        val retention = Retention.Days(3)

        assertThat(retention.cutoffFrom(now)).isEqualTo(now.minusDays(3))
        assertThat(retention.isAccessible(now.minusDays(2), now)).isTrue()
        assertThat(retention.isAccessible(now.minusDays(4), now)).isFalse()
        assertThat(retention.isAccessible(null, now)).isTrue()
    }

    @Test
    @DisplayName("Unlimited는 cutoff가 없고 항상 접근 가능하다")
    fun unlimited() {
        assertThat(Retention.Unlimited.cutoffFrom(now)).isNull()
        assertThat(Retention.Unlimited.isAccessible(now.minusYears(10), now)).isTrue()
    }
}