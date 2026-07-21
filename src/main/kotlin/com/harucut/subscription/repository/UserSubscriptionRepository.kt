package com.harucut.subscription.repository

import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.enums.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserSubscriptionRepository : JpaRepository<UserSubscription, Long> {

    fun findByUserId(userId: Long): UserSubscription?

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserSubscription us WHERE us.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)

    // 자동갱신 대상: ACTIVE + autoRenew + 결제 주기가 now 이전에 끝난 구독
    @Query(
        """
            select us.id
            from UserSubscription us
            where us.status = :status
            and us.autoRenew = true
            and us.currentPeriodEnd <= :now
        """
    )
    fun findRenewableIds(
        @Param("status") status: SubscriptionStatus,
        @Param("now") now: LocalDateTime
    ): List<Long>

    // 만료 강등 대상: 주어진 상태이고 결제 주기가 cutoff 이전에 끝난 구독
    @Query(
        """
            select us.id
            from UserSubscription us
            where us.status = :status
            and us.currentPeriodEnd <= :cutoff
        """
    )
    fun findExpirableIds(
        @Param("status") status: SubscriptionStatus,
        @Param("cutoff") cutoff: LocalDateTime
    ): List<Long>
}