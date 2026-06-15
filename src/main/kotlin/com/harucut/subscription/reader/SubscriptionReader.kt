package com.harucut.subscription.reader

import com.harucut.exception.BusinessException
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Component

@Component
class SubscriptionReader(
    private val userSubscriptionRepository: UserSubscriptionRepository
) {
    fun getByUser(user: User): UserSubscription =
        userSubscriptionRepository.findByUser(user)
            ?: throw BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND)

    fun getByUserId(userId: Long): UserSubscription =
        userSubscriptionRepository.findByUserId(userId)
            ?: throw BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND)

    fun findByUser(user: User): UserSubscription? =
        userSubscriptionRepository.findByUser(user)
}