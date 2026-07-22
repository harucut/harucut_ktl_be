package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SubscriptionServiceImpl(
    private val userSubscriptionRepository: UserSubscriptionRepository
) : SubscriptionService {

    @Transactional(readOnly = true)
    override fun getMySubscription(userId: Long): SubscriptionResponse =
        SubscriptionResponse.from(getSubscriptionOrThrow(userId))

    override fun cancelAutoRenew(userId: Long) {
        val subscription = getSubscriptionOrThrow(userId)
        if (subscription.status == SubscriptionStatus.CANCELED) {
            throw BusinessException(SubscriptionErrorCode.ALREADY_CANCELED)
        }
        subscription.cancelAutoRenew()
    }

    private fun getSubscriptionOrThrow(userId: Long): UserSubscription =
        userSubscriptionRepository.findByUserId(userId)
            ?: throw BusinessException(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)
}
