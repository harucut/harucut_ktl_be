package com.harucut.auth.exit.batch.scheduler

import com.harucut.auth.exit.batch.service.UserExitBatchService
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
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

class UserDeletionSchedulerTest {

    private val userRepository: UserRepository = mockk()
    private val batchService: UserExitBatchService = mockk()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC)

    private val scheduler = UserDeletionScheduler(userRepository, batchService, fixedClock)

    @Nested
    @DisplayName("run")
    inner class Run {

        @Test
        @DisplayName("유예기간이 지난 유저들을 각각 독립 트랜잭션으로 하드삭제한다")
        fun success() {
            // given
            val expectedThreshold = LocalDateTime.now(fixedClock).minusDays(7)
            every {
                userRepository.findExpiredDeleteRequestedUserIds(UserStatus.DELETED_REQUESTED, expectedThreshold)
            } returns listOf(1L, 2L, 3L)
            every { batchService.exitInNewTransaction(any()) } just runs

            // when
            scheduler.run()

            // then
            verify { userRepository.findExpiredDeleteRequestedUserIds(UserStatus.DELETED_REQUESTED, expectedThreshold) }
            verify { batchService.exitInNewTransaction(1L) }
            verify { batchService.exitInNewTransaction(2L) }
            verify { batchService.exitInNewTransaction(3L) }
        }

        @Test
        @DisplayName("한 유저 처리 중 예외가 발생해도 나머지 유저는 계속 처리한다")
        fun continuesOnError() {
            // given
            every {
                userRepository.findExpiredDeleteRequestedUserIds(any(), any())
            } returns listOf(1L, 2L, 3L)
            every { batchService.exitInNewTransaction(any()) } just runs
            every { batchService.exitInNewTransaction(2L) } throws RuntimeException("삭제 실패")

            // when & then
            assertThatCode { scheduler.run() }.doesNotThrowAnyException()

            verify { batchService.exitInNewTransaction(1L) }
            verify { batchService.exitInNewTransaction(3L) }
        }

        @Test
        @DisplayName("대상 유저가 없으면 배치를 호출하지 않는다")
        fun noTargets() {
            // given
            every {
                userRepository.findExpiredDeleteRequestedUserIds(any(), any())
            } returns emptyList()

            // when
            scheduler.run()

            // then
            verify(exactly = 0) { batchService.exitInNewTransaction(any()) }
        }
    }

}