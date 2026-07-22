package com.harucut.payment.batch.scheduler

import com.harucut.payment.batch.service.SubscriptionRenewalBatchService
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SubscriptionRenewalSchedulerTest {

    private val userSubscriptionRepository: UserSubscriptionRepository = mockk()
    private val batchService: SubscriptionRenewalBatchService = mockk()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)

    private val scheduler = SubscriptionRenewalScheduler(userSubscriptionRepository, batchService, fixedClock)

    @Nested
    @DisplayName("run")
    inner class Run {

        @Test
        @DisplayName("갱신 대상 구독을 각각 독립 트랜잭션으로 재청구한다")
        fun success() {
            val now = LocalDateTime.now(fixedClock)
            every { userSubscriptionRepository.findRenewableIds(SubscriptionStatus.ACTIVE, now) } returns listOf(1L, 2L, 3L)
            every { batchService.renewInNewTransaction(any()) } just runs

            scheduler.run()

            verify { batchService.renewInNewTransaction(1L) }
            verify { batchService.renewInNewTransaction(2L) }
            verify { batchService.renewInNewTransaction(3L) }
        }

        @Test
        @DisplayName("한 구독 처리 중 예외가 발생해도 나머지 구독은 계속 처리한다")
        fun continuesOnError() {
            every { userSubscriptionRepository.findRenewableIds(any(), any()) } returns listOf(1L, 2L, 3L)
            every { batchService.renewInNewTransaction(any()) } just runs
            every { batchService.renewInNewTransaction(2L) } throws RuntimeException("갱신 실패")

            assertThatCode { scheduler.run() }.doesNotThrowAnyException()

            verify { batchService.renewInNewTransaction(1L) }
            verify { batchService.renewInNewTransaction(3L) }
        }

        @Test
        @DisplayName("대상 구독이 없으면 배치를 호출하지 않는다")
        fun noTargets() {
            every { userSubscriptionRepository.findRenewableIds(any(), any()) } returns emptyList()

            scheduler.run()

            verify(exactly = 0) { batchService.renewInNewTransaction(any()) }
        }
    }
}
