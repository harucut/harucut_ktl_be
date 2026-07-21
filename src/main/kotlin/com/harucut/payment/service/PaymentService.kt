package com.harucut.payment.service

import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.plan.PlanTier

interface PaymentService {

    // 빌링키 발급 + 최초 결제 → 성공 시 구독을 유료로 활성화한다.
    fun subscribe(userId: Long, planTier: PlanTier, customerKey: String, authKey: String): SubscriptionResponse
}
