package com.harucut.payment.entity

import com.harucut.payment.enums.OrderStatus
import com.harucut.payment.enums.OrderType
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*

// 가맹점 주문 (테이블명은 order 예약어 회피를 위해 payment_order)
@Entity
@Table(
    name = "payment_order",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payment_order_idempotency_key", columnNames = ["idempotency_key"])
    ],
    indexes = [
        Index(name = "idx_payment_order_user_id", columnList = "user_id")
    ]
)
class PaymentOrder(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_tier", nullable = false, length = 16)
    val targetTier: PlanTier,

    @Column(nullable = false)
    val amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 16)
    val orderType: OrderType,

    @Column(name = "idempotency_key", nullable = false, length = 100)
    val idempotencyKey: String
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_order_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: OrderStatus = OrderStatus.CREATED
        protected set

    fun markPaid() {
        this.status = OrderStatus.PAID
    }

    fun markFailed() {
        this.status = OrderStatus.FAILED
    }
}
