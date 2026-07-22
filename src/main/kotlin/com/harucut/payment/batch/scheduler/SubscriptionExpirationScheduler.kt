package com.harucut.payment.batch.scheduler

import com.harucut.payment.batch.service.SubscriptionExpirationBatchService
import com.harucut.payment.config.PaymentProperties
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

// 해지 예약(CANCELED)이 만료 시점을 지났거나, 연체(PAST_DUE)가 유예기간을 초과한 구독을 BASIC으로 강등한다.
@Component
class SubscriptionExpirationScheduler(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val subscriptionExpirationBatchService: SubscriptionExpirationBatchService,
    private val paymentProperties: PaymentProperties,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 1 * * *")
    fun run() {
        val now = LocalDateTime.now(clock)
        val graceCutoff = now.minusDays(paymentProperties.graceDays)

        val expirableIds = userSubscriptionRepository.findExpirableIds(SubscriptionStatus.CANCELED, now) +
            userSubscriptionRepository.findExpirableIds(SubscriptionStatus.PAST_DUE, graceCutoff)

        expirableIds.forEach { subscriptionId ->
            try {
                log.info("subscriptionId={} 구독 만료(BASIC 강등) 처리 시작", subscriptionId)
                subscriptionExpirationBatchService.expireInNewTransaction(subscriptionId)
            } catch (e: Exception) {
                log.warn("[구독 만료 처리 예외] subscriptionId={}", subscriptionId, e)
            }
        }
    }
}
