package com.harucut.subscription.service

import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.plan.PlanTier

interface SubscriptionAdminService {

    fun getSubscription(userId: Long): SubscriptionResponse

    // 결제 없이 관리자가 직접 요금제를 변경하는 운영 오버라이드
    fun changePlan(userId: Long, planTier: PlanTier)
}
