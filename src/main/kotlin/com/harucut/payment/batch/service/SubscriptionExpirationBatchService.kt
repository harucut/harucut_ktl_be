package com.harucut.payment.batch.service

import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SubscriptionExpirationBatchService(
    private val userSubscriptionRepository: UserSubscriptionRepository
) {

    // 해지 예약 후 만료되었거나(CANCELED), 연체 유예기간을 초과한(PAST_DUE) 구독을 무료로 강등한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun expireInNewTransaction(subscriptionId: Long) {
        val subscription = userSubscriptionRepository.findById(subscriptionId).orElse(null) ?: return
        subscription.expireToFree()
    }
}
