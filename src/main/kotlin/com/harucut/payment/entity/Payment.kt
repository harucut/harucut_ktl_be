package com.harucut.payment.entity

import com.harucut.payment.enums.PaymentMethod
import com.harucut.payment.enums.PaymentStatus
import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

// 결제 시도/결과 (한 주문에 대해 여러 행이 쌓일 수 있음)
@Entity
@Table(
    name = "payment",
    indexes = [
        Index(name = "idx_payment_order_id", columnList = "payment_order_id")
    ]
)
class Payment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    val order: PaymentOrder,

    @Column(nullable = false)
    val amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val method: PaymentMethod
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: PaymentStatus = PaymentStatus.REQUESTED
        protected set

    @Column(name = "pg_transaction_id", length = 100)
    var pgTransactionId: String? = null
        protected set

    @Column(name = "failure_code", length = 50)
    var failureCode: String? = null
        protected set

    @Column(name = "failure_message", length = 255)
    var failureMessage: String? = null
        protected set

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null
        protected set

    fun approve(pgTransactionId: String, approvedAt: LocalDateTime) {
        this.status = PaymentStatus.APPROVED
        this.pgTransactionId = pgTransactionId
        this.approvedAt = approvedAt
    }

    fun fail(failureCode: String?, failureMessage: String?) {
        this.status = PaymentStatus.FAILED
        this.failureCode = failureCode
        this.failureMessage = failureMessage
    }
}
