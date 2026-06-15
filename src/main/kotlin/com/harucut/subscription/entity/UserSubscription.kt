package com.harucut.subscription.entity

import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.user.entity.User
import com.harucut.user.enums.PlanTier
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_subscription",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_subscription_user_id", columnNames = ["user_id"])]
)
class UserSubscription : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 16)
    var planTier: PlanTier = PlanTier.BASIC

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 16)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE

    @Column(name = "current_cycle_start_at", nullable = false)
    lateinit var currentCycleStartAt: LocalDateTime

    @Column(name = "current_cycle_end_at", nullable = false)
    lateinit var currentCycleEndAt: LocalDateTime

    /** 이번 사이클 동영상 변환 사용 횟수 */
    @Column(name = "current_transcode_count", nullable = false)
    var currentTranscodeCount: Int = 0

    @Column(name = "auto_renew", nullable = false)
    var autoRenew: Boolean = true

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

    @OneToMany(mappedBy = "subscription", cascade = [CascadeType.ALL], orphanRemoval = true)
    var usageCycles: MutableList<SubscriptionUsageCycle> = mutableListOf()

    // ── 플랜 변경 ──────────────────────────────────────────
    fun changePlanTier(planTier: PlanTier) {
        this.planTier = planTier
    }

    // ── 구독 상태 ──────────────────────────────────────────
    fun cancel() {
        status = SubscriptionStatus.CANCELED
    }

    fun expire() {
        status = SubscriptionStatus.EXPIRED
    }

    fun activate() {
        status = SubscriptionStatus.ACTIVE
    }

    // ── 사이클 관리 ────────────────────────────────────────
    /**
     * [now]가 현재 사이클 종료 이후면 사이클을 갱신하고 트랜스코딩 카운터를 초기화한다.
     * 서비스에서 Clock.now()를 받아 넘긴다.
     */
    fun syncQuotaCycle(now: LocalDateTime) {
        if (now.isBefore(currentCycleEndAt)) return

        while (!now.isBefore(currentCycleEndAt)) {
            currentCycleStartAt = currentCycleEndAt
            currentCycleEndAt = currentCycleEndAt.plusDays(31)
        }
        currentTranscodeCount = 0
    }

    fun startNewQuotaCycle(startAt: LocalDateTime) {
        currentCycleStartAt = startAt
        currentCycleEndAt = startAt.plusDays(31)
        currentTranscodeCount = 0
    }

    // ── 사용량 ──────────────────────────────────────────────
    fun increaseTranscodeCount() {
        currentTranscodeCount++
    }

    fun isTranscodeLimitReached(): Boolean =
        !planTier.isTranscodeUnlimited && currentTranscodeCount >= planTier.monthlyTranscodeLimit

    // ── 팩토리 ──────────────────────────────────────────────
    companion object {
        fun createDefault(user: User, now: LocalDateTime): UserSubscription {
            val subscription = UserSubscription().apply {
                this.user = user
                this.currentCycleStartAt = now
                this.currentCycleEndAt = now.plusDays(31)
            }
            user.attachSubscription(subscription)
            return subscription
        }
    }
}