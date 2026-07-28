package com.harucut.coupon.entity

import com.harucut.coupon.enums.UserCouponStatus
import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_coupon",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_coupon_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uk_user_coupon_user_coupon", columnNames = ["user_id", "coupon_id"])
    ],
    indexes = [
        Index(name = "idx_user_coupon_user_id", columnList = "user_id")
    ]
)
class UserCoupon(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    val coupon: Coupon,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: UserCouponStatus,

    @Column(name = "redeemed_at", nullable = false)
    val redeemedAt: LocalDateTime
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    // 예약된 grant를 개시 처리 (RESERVED -> REDEEMED)
    fun markRedeemed() {
        this.status = UserCouponStatus.REDEEMED
    }

    companion object {
        // 현 주기 후 예약
        fun reserved(coupon: Coupon, userId: Long, now: LocalDateTime): UserCoupon =
            UserCoupon(userId = userId, coupon = coupon, status = UserCouponStatus.RESERVED, redeemedAt = now)

        // 즉시 개시
        fun redeemed(coupon: Coupon, userId: Long, now: LocalDateTime): UserCoupon =
            UserCoupon(userId = userId, coupon = coupon, status = UserCouponStatus.REDEEMED, redeemedAt = now)
    }
}
