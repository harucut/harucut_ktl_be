package com.harucut.payment.batch.service

import com.harucut.coupon.service.GrantActivationService
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SubscriptionExpirationBatchService(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val grantActivationService: GrantActivationService
) {

    // 해지 예약 후 만료되었거나(CANCELED), 연체 유예기간을 초과한(PAST_DUE), 무료 grant 기간이 만료된(GRANTED) 구독을 강등한다.
    // 단, 현 주기 후로 예약된 무료 grant가 있으면 강등 대신 그 grant를 활성화한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun expireInNewTransaction(subscriptionId: Long, now: LocalDateTime) {
        val subscription = userSubscriptionRepository.findById(subscriptionId).orElse(null) ?: return
        if (subscription.reservedGrantCouponId != null) {
            grantActivationService.activate(subscription, now)
        } else {
            subscription.expireToFree()
        }
    }
}
