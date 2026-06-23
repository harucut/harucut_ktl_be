package com.harucut.subscription.entity

import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserSubscriptionTest {

    private fun user(): User = mockk(relaxed = true)

    @Nested
    inner class CreateDefault {

        @Test
        @DisplayName("기본 구독은 BASIC 요금제, 업로드 0회, 1개월 사이클로 생성된다")
        fun success() {
            val before = LocalDateTime.now()

            val sub = UserSubscription.createDefault(user())

            assertThat(sub.planTier).isEqualTo(PlanTier.BASIC)
            assertThat(sub.currentVideoUploadCount).isZero()
            assertThat(sub.currentCycleEndAt).isAfterOrEqualTo(before.plusMonths(1).minusSeconds(5))
        }
    }

    @Nested
    inner class SyncQuotaCycle {

        @Test
        @DisplayName("사이클이 아직 안 끝났으면 카운트와 종료 시각을 유지한다")
        fun within() {
            val sub = UserSubscription.createDefault(user())
            sub.increaseVideoUploadCount()
            val end = sub.currentCycleEndAt

            sub.syncQuotaCycle(LocalDateTime.now())

            assertThat(sub.currentVideoUploadCount).isEqualTo(1)
            assertThat(sub.currentCycleEndAt).isEqualTo(end)
        }

        @Test
        @DisplayName("사이클이 끝났으면 다음 창으로 이동하고 카운트를 리셋한다")
        fun expired() {
            val sub = UserSubscription.createDefault(user())
            sub.increaseVideoUploadCount()
            val now = sub.currentCycleEndAt.plusDays(1)

            sub.syncQuotaCycle(now)

            assertThat(sub.currentVideoUploadCount).isZero()
            assertThat(sub.currentCycleEndAt).isAfter(now)
            assertThat(sub.currentCycleStartAt).isBeforeOrEqualTo(now)
        }

        @Test
        @DisplayName("여러 사이클이 밀려도 현재 시각을 포함하는 창까지 이동한다")
        fun multiCycle() {
            val sub = UserSubscription.createDefault(user())
            val now = sub.currentCycleEndAt.plusMonths(3).plusDays(1)

            sub.syncQuotaCycle(now)

            assertThat(sub.currentCycleStartAt).isBeforeOrEqualTo(now)
            assertThat(sub.currentCycleEndAt).isAfter(now)
        }
    }

}