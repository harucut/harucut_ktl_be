package com.harucut.payment.batch.scheduler

import com.harucut.payment.batch.service.SubscriptionRenewalBatchService
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class SubscriptionRenewalScheduler(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val subscriptionRenewalBatchService: SubscriptionRenewalBatchService,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 1 * * *")
    fun run() {
        val now = LocalDateTime.now(clock)
        val subscriptionIds = userSubscriptionRepository.findRenewableIds(SubscriptionStatus.ACTIVE, now)

        subscriptionIds.forEach { subscriptionId ->
            try {
                log.info("subscriptionId={} 정기결제 갱신 시작", subscriptionId)
                subscriptionRenewalBatchService.renewInNewTransaction(subscriptionId)
            } catch (e: Exception) {
                log.warn("[정기결제 갱신 예외] subscriptionId={}", subscriptionId, e)
            }
        }
    }
}
