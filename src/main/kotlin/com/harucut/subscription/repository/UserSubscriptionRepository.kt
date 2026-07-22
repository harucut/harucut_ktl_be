package com.harucut.subscription.repository

import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserSubscriptionRepository : JpaRepository<UserSubscription, Long> {

    fun findByUserId(userId: Long): UserSubscription?

    // 관리자 통계 스냅샷: status별 구독 수
    fun countByStatus(status: SubscriptionStatus): Long

    // 관리자 통계 스냅샷: tier별 "현재 유료 접근 중"인 구독 수.
    // 자동갱신 해지 예약(CANCELED)이어도 currentPeriodEnd까지는 유료 등급 접근이 유지되므로
    // ACTIVE와 CANCELED(만료 전)를 함께 센다(UserSubscription.effectiveTier 참고).
    fun countByStatusInAndPlanTierAndCurrentPeriodEndAfter(
        statuses: Collection<SubscriptionStatus>,
        planTier: PlanTier,
        now: LocalDateTime
    ): Long

    // 관리자 통계 스냅샷: 자동갱신 on/off 구독 수
    fun countByAutoRenew(autoRenew: Boolean): Long

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