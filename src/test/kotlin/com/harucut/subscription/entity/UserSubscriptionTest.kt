package com.harucut.subscription.entity

import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserSubscriptionTest {

    private fun user(): User = mockk(relaxed = true)

    @Nested
    inner class CreateDefault {

        @Test
        @DisplayName("기본 구독은 BASIC 요금제로 생성된다")
        fun success() {
            val sub = UserSubscription.createDefault(user())

            assertThat(sub.planTier).isEqualTo(PlanTier.BASIC)
        }
    }
}
