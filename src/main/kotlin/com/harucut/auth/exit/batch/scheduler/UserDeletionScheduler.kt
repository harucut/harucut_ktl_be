package com.harucut.auth.exit.batch.scheduler

import com.harucut.auth.exit.batch.service.UserExitBatchService
import com.harucut.user.entity.User
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class UserDeletionScheduler(
    private val userRepository: UserRepository,
    private val userExitBatchService: UserExitBatchService,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 * * *")
    fun run() {
        val threshold = LocalDateTime.now(clock).minusDays(User.DELETION_GRACE_DAYS)

        val userIds = userRepository.findExpiredDeleteRequestedUserIds(
            UserStatus.DELETED_REQUESTED,
            threshold
        )

        userIds.forEach { userId ->
            try {
                log.info("userId={} 탈퇴 배치 처리 시작", userId)
                userExitBatchService.exitInNewTransaction(userId)
            } catch (e: Exception) {
                log.warn("[탈퇴 일괄처리 예외] userId={}", userId, e)
            }
        }
    }
}