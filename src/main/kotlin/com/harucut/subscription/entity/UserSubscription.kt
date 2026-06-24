package com.harucut.subscription.entity

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

    @Column(name = "current_cycle_start_at", nullable = false)
    var currentCycleStartAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "current_cycle_end_at", nullable = false)
    var currentCycleEndAt: LocalDateTime = LocalDateTime.now().plusMonths(1)
        protected set

    @Column(name = "current_video_upload_count", nullable = false)
    var currentVideoUploadCount: Int = 0
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set


    /** 만료된 사이클을 현재 시각이 포함되는 창으로 이동시키고 업로드 카운트를 0으로 리셋한다. */
    fun syncQuotaCycle(now: LocalDateTime) {
        if (now.isBefore(currentCycleEndAt)) {
            return
        }
        val synced = previewCycle(now)
        currentCycleStartAt = synced.start
        currentCycleEndAt = synced.end
        currentVideoUploadCount = synced.videoUploadCount
    }

    /**
     * 현재 시각 기준 사이클 스냅샷을 계산해 반환한다(상태를 변경하지 않음).
     * 만료된 경우 현재 시각이 포함되는 다음 창과 사용량 0을 돌려주어, 영속화 없이 표시용으로 쓸 수 있다.
     */
    fun previewCycle(now: LocalDateTime): CycleView {
        if (now.isBefore(currentCycleEndAt)) {
            return CycleView(currentCycleStartAt, currentCycleEndAt, currentVideoUploadCount)
        }
        var start = currentCycleStartAt
        var end = currentCycleEndAt
        while (!now.isBefore(end)) {
            start = end
            end = end.plusMonths(1)
        }
        return CycleView(start, end, 0)
    }

    /** 현재 사이클의 동영상 업로드 사용 횟수를 1 증가시킨다. */
    fun increaseVideoUploadCount() {
        currentVideoUploadCount++
    }

    companion object {
        /** 신규 가입자의 기본(무료 BASIC) 구독을 생성한다. */
        fun createDefault(user: User): UserSubscription = UserSubscription(user = user)
    }
}