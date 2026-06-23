package com.harucut.subscription.event

import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.event.UserRegisteredEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SubscriptionProvisioningListener(
    private val userSubscriptionRepository: UserSubscriptionRepository
) {

    /** 가입 이벤트를 같은 트랜잭션에서 받아 기본(무료) 구독을 생성한다. */
    @EventListener
    fun handleUserRegistered(event: UserRegisteredEvent) {
        val userId = event.user.id ?: return
        if (userSubscriptionRepository.findByUserId(userId) == null) {
            userSubscriptionRepository.save(UserSubscription.createDefault(event.user))
        }
    }
}