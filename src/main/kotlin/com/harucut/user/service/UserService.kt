package com.harucut.user.service

import com.harucut.subscription.plan.PlanTier
import com.harucut.user.dto.SubscriptionUsageResponse
import com.harucut.user.dto.UserInfoResponse

interface UserService {

    fun getUserInfo(userId: Long): UserInfoResponse

    fun getSubscriptionUsage(userId: Long): SubscriptionUsageResponse

    fun changeUsername(userId: Long, username: String)

    fun changeProfileImage(userId: Long, s3Key: String)

    fun changePlan(userId: Long, planTier: PlanTier)
}
