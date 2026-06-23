package com.harucut.subscription.repository

import com.harucut.subscription.entity.UserSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserSubscriptionRepository : JpaRepository<UserSubscription, Long> {

    fun findByUserId(userId: Long): UserSubscription?

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserSubscription us WHERE us.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)
}