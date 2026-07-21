package com.harucut.subscription.service

import com.harucut.exception.BusinessException
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SubscriptionAdminServiceImpl(
    private val userSubscriptionRepository: UserSubscriptionRepository
) : SubscriptionAdminService {

    @Transactional(readOnly = true)
    override fun getSubscription(userId: Long): SubscriptionResponse =
        SubscriptionResponse.from(getSubscriptionOrThrow(userId))

    override fun changePlan(userId: Long, planTier: PlanTier) {
        getSubscriptionOrThrow(userId).changePlan(planTier)
    }

    private fun getSubscriptionOrThrow(userId: Long) =
        userSubscriptionRepository.findByUserId(userId)
            ?: throw BusinessException(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)
}
