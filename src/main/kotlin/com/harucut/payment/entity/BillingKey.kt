package com.harucut.payment.entity

import com.harucut.payment.enums.BillingKeyStatus
import com.harucut.payment.enums.PgProvider
import com.harucut.user.entity.User
import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "billing_key",
    indexes = [
        Index(name = "idx_billing_key_user_id", columnList = "user_id")
    ]
)
class BillingKey(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val provider: PgProvider,

    @Column(name = "billing_key_value", nullable = false, length = 255)
    val billingKeyValue: String,

    @Column(name = "masked_card", length = 32)
    val maskedCard: String? = null
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_key_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: BillingKeyStatus = BillingKeyStatus.ACTIVE
        protected set

    // PG 빌링키 폐기 처리
    fun delete() {
        this.status = BillingKeyStatus.DELETED
    }
}
