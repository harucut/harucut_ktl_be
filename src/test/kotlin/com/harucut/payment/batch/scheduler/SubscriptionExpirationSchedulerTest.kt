package com.harucut.payment.batch.scheduler

import com.harucut.payment.batch.service.SubscriptionExpirationBatchService
import com.harucut.payment.config.PaymentProperties
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

class SubscriptionExpirationSchedulerTest {

    private val userSubscriptionRepository: UserSubscriptionRepository = mockk()
    private val batchService: SubscriptionExpirationBatchService = mockk()
    private val paymentProperties = PaymentProperties()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)

    private val scheduler = SubscriptionExpirationScheduler(
        userSubscriptionRepository, batchService, paymentProperties, fixedClock
    )

    @Nested
    @DisplayName("run")
    inner class Run {

        @Test
        @DisplayName("해지예약(CANCELED)·연체(PAST_DUE)·무료grant(GRANTED) 만료 대상을 각각 조회해 강등 처리한다")
        fun success() {
            val now = LocalDateTime.now(fixedClock)
            val graceCutoff = now.minusDays(paymentProperties.graceDays)
            every { userSubscriptionRepository.findExpirableIds(SubscriptionStatus.CANCELED, now) } returns listOf(1L)
            every { userSubscriptionRepository.findExpirableIds(SubscriptionStatus.PAST_DUE, graceCutoff) } returns listOf(2L)
            every { userSubscriptionRepository.findExpirableIds(SubscriptionStatus.GRANTED, now) } returns listOf(3L)
            every { batchService.expireInNewTransaction(any()) } just runs

            scheduler.run()

            verify { batchService.expireInNewTransaction(1L) }
            verify { batchService.expireInNewTransaction(2L) }
            verify { batchService.expireInNewTransaction(3L) }
        }

        @Test
        @DisplayName("한 구독 처리 중 예외가 발생해도 나머지 구독은 계속 처리한다")
        fun continuesOnError() {
            every { userSubscriptionRepository.findExpirableIds(any(), any()) } returns listOf(1L, 2L, 3L)
            every { batchService.expireInNewTransaction(any()) } just runs
            every { batchService.expireInNewTransaction(2L) } throws RuntimeException("만료 실패")

            assertThatCode { scheduler.run() }.doesNotThrowAnyException()

            verify { batchService.expireInNewTransaction(1L) }
            verify { batchService.expireInNewTransaction(3L) }
        }

        @Test
        @DisplayName("대상 구독이 없으면 배치를 호출하지 않는다")
        fun noTargets() {
            every { userSubscriptionRepository.findExpirableIds(any(), any()) } returns emptyList()

            scheduler.run()

            verify(exactly = 0) { batchService.expireInNewTransaction(any()) }
        }
    }
}
