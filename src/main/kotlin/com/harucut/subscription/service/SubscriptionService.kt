package com.harucut.subscription.service

import com.harucut.subscription.dto.SubscriptionResponse

interface SubscriptionService {

    fun getMySubscription(userId: Long): SubscriptionResponse

    // 자동갱신 해지 예약 (만료 시점까지는 현재 요금제를 유지)
    fun cancelAutoRenew(userId: Long)
}
