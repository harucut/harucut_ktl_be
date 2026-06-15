package com.harucut.subscription.repository

import com.harucut.subscription.entity.UserSubscription
import com.harucut.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSubscriptionRepository : JpaRepository<UserSubscription, Long> {

    fun findByUser(user: User): UserSubscription?

    fun findByUserId(userId: Long): UserSubscription?
}