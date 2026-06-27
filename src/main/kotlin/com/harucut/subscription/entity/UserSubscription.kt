package com.harucut.subscription.entity

import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*

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

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    companion object {
        fun createDefault(user: User): UserSubscription = UserSubscription(user = user)
    }
}
