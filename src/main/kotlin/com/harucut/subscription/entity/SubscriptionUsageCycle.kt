package com.harucut.subscription.entity

import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "subscription_usage_cycle",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_subscription_cycle_start",
            columnNames = ["subscription_id", "cycle_start_at"]
        )
    ]
)
class SubscriptionUsageCycle : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_cycle_id")
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    lateinit var subscription: UserSubscription

    @Column(name = "cycle_start_at", nullable = false)
    lateinit var cycleStartAt: LocalDateTime

    @Column(name = "cycle_end_at", nullable = false)
    lateinit var cycleEndAt: LocalDateTime

    /** 해당 사이클 동영상 변환 사용 횟수 스냅샷 */
    @Column(name = "transcode_count", nullable = false)
    var transcodeCount: Int = 0

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

    companion object {
        fun snapshot(
            subscription: UserSubscription,
            cycleStartAt: LocalDateTime,
            cycleEndAt: LocalDateTime,
            transcodeCount: Int
        ): SubscriptionUsageCycle = SubscriptionUsageCycle().apply {
            this.subscription  = subscription
            this.cycleStartAt  = cycleStartAt
            this.cycleEndAt    = cycleEndAt
            this.transcodeCount = transcodeCount
        }
    }
}