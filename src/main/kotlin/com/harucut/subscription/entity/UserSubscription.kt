package com.harucut.subscription.entity

import com.harucut.payment.entity.BillingKey
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_subscription",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_subscription_user_id", columnNames = ["user_id"])
    ]
)
class UserSubscription(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 16)
    var planTier: PlanTier = PlanTier.DEFAULT
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE
        protected set

    // 유료 구독의 현재 결제 주기. 무료(BASIC)는 무기한이므로 null.
    @Column(name = "current_period_start")
    var currentPeriodStart: LocalDateTime? = null
        protected set

    @Column(name = "current_period_end")
    var currentPeriodEnd: LocalDateTime? = null
        protected set

    @Column(name = "auto_renew", nullable = false)
    var autoRenew: Boolean = false
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id")
    var billingKey: BillingKey? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    // 요금제 단계 변경 (관리자 운영 오버라이드 전용 — 기간/결제 상태는 건드리지 않음)
    fun changePlan(tier: PlanTier) {
        this.planTier = tier
    }

    // 결제 성공으로 유료 구독 최초 활성화
    fun activatePaid(tier: PlanTier, start: LocalDateTime, end: LocalDateTime, billingKey: BillingKey) {
        this.planTier = tier
        this.status = SubscriptionStatus.ACTIVE
        this.currentPeriodStart = start
        this.currentPeriodEnd = end
        this.autoRenew = true
        this.billingKey = billingKey
    }

    // 정기결제 성공으로 다음 결제 주기로 갱신
    fun renew(start: LocalDateTime, end: LocalDateTime) {
        this.status = SubscriptionStatus.ACTIVE
        this.currentPeriodStart = start
        this.currentPeriodEnd = end
    }

    // 정기결제 실패 → 연체(PAST_DUE) 표시. 재시도 청구는 없다 — 접근 권한은 effectiveTier()가
    // 결제 주기 종료 시점에 이미 BASIC으로 즉시 제한하며, 유예기간은 오직 만료(BASIC 강등) 처리를
    // 늦추는 기록용 대기시간일 뿐이다(SubscriptionExpirationScheduler 참고).
    fun markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE
    }

    // 자동갱신 해지 예약 (만료 시점까지는 유료 등급 유지)
    fun cancelAutoRenew() {
        this.autoRenew = false
        this.status = SubscriptionStatus.CANCELED
    }

    // 만료 처리 → 무료(BASIC)로 강등, 결제 정보 정리
    fun expireToFree() {
        this.planTier = PlanTier.BASIC
        this.status = SubscriptionStatus.EXPIRED
        this.currentPeriodStart = null
        this.currentPeriodEnd = null
        this.autoRenew = false
        this.billingKey = null
    }

    // 정책 적용에 사용할 실질 요금제. 유료 구독의 결제 주기가 지났는데
    // 아직 배치가 강등 처리를 못 했다면(공백기) BASIC 으로 취급해 접근을 보수적으로 제한한다.
    fun effectiveTier(now: LocalDateTime): PlanTier {
        if (planTier == PlanTier.BASIC) return PlanTier.BASIC
        val end = currentPeriodEnd ?: return planTier
        return if (now.isBefore(end)) planTier else PlanTier.BASIC
    }

    companion object {
        fun createDefault(user: User): UserSubscription = UserSubscription(user = user)
    }
}
