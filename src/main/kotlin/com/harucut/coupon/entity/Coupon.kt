package com.harucut.coupon.entity

import com.harucut.subscription.plan.PlanTier
import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "coupon",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_coupon_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uk_coupon_code", columnNames = ["code"])
    ]
)
class Coupon(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 32)
    val code: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_tier", nullable = false, length = 16)
    val grantTier: PlanTier,

    @Column(name = "max_redemptions")
    val maxRedemptions: Int? = null,

    @Column(name = "valid_until")
    val validUntil: LocalDateTime? = null
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    @Column(nullable = false)
    var active: Boolean = true
        protected set

    // 관리자 kill switch
    fun deactivate() {
        this.active = false
    }

    // 사용 가능 여부: 활성 + (마감 없음 또는 마감 전)
    fun isRedeemable(now: LocalDateTime): Boolean =
        active && (validUntil == null || now.isBefore(validUntil))
}
