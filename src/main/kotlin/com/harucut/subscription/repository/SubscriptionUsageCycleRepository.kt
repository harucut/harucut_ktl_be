package com.harucut.subscription.repository

import com.harucut.subscription.entity.SubscriptionUsageCycle
import com.harucut.subscription.entity.UserSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubscriptionUsageCycleRepository : JpaRepository<SubscriptionUsageCycle, Long> {

    fun findAllBySubscriptionOrderByCycleStartAtDesc(subscription: UserSubscription): List<SubscriptionUsageCycle>
}