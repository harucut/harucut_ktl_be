package com.harucut.subscription.entity

import com.harucut.subscription.enums.PaymentEventType
import com.harucut.subscription.enums.PaymentStatus
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "payment_transaction",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_payment_provider_event",
            columnNames = ["provider", "provider_event_id"]
        )
    ]
)
class PaymentTransaction : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_tx_id")
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    lateinit var subscription: UserSubscription

    @Column(name = "provider", nullable = false, length = 32)
    lateinit var provider: String

    @Column(name = "provider_event_id", nullable = false, length = 128)
    lateinit var providerEventId: String

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    lateinit var eventType: PaymentEventType

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 16)
    lateinit var paymentStatus: PaymentStatus

    @Column(name = "amount", nullable = false)
    var amount: Int = 0

    @Column(name = "currency", nullable = false, length = 8)
    lateinit var currency: String

    @Column(name = "occurred_at", nullable = false)
    lateinit var occurredAt: LocalDateTime

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    var rawPayload: String = ""

    companion object {
        fun create(
            subscription: UserSubscription,
            provider: String,
            providerEventId: String,
            eventType: PaymentEventType,
            paymentStatus: PaymentStatus,
            amount: Int,
            currency: String,
            occurredAt: LocalDateTime,
            rawPayload: String = ""
        ): PaymentTransaction = PaymentTransaction().apply {
            this.subscription = subscription
            this.provider = provider
            this.providerEventId = providerEventId
            this.eventType = eventType
            this.paymentStatus = paymentStatus
            this.amount = amount
            this.currency = currency
            this.occurredAt = occurredAt
            this.rawPayload = rawPayload
        }
    }
}