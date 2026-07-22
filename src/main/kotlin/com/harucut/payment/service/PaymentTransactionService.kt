package com.harucut.payment.service

import com.harucut.payment.entity.BillingKey
import com.harucut.payment.entity.Payment
import com.harucut.payment.entity.PaymentOrder
import com.harucut.payment.enums.OrderType
import com.harucut.payment.enums.PaymentMethod
import com.harucut.payment.enums.PgProvider
import com.harucut.payment.gateway.dto.PaymentResult
import com.harucut.payment.repository.BillingKeyRepository
import com.harucut.payment.repository.PaymentOrderRepository
import com.harucut.payment.repository.PaymentRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// PG 외부 호출 전후로 DB 커밋 경계를 분리하기 위한 헬퍼.
// "PG 성공 + DB 기록 실패"로 결제 이력이 유실되는 상황을 막기 위해
// 주문 생성과 결제 결과 반영을 각각 독립 트랜잭션(REQUIRES_NEW)으로 커밋한다.
@Service
class PaymentTransactionService(
    private val userRepository: UserRepository,
    private val billingKeyRepository: BillingKeyRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentRepository: PaymentRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository
) {

    data class CreatedOrder(val orderId: Long, val orderPublicId: String, val billingKeyId: Long)

    // subscription은 청구 성공 시에만 채워진다. 실패해도 이 트랜잭션은 그대로 커밋되어
    // 실패 이력(Payment/PaymentOrder)이 보존된다 — 예외를 던지지 않는 이유.
    data class ChargeApplyResult(val success: Boolean, val subscription: UserSubscription?)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createInitialOrderInNewTransaction(
        userId: Long,
        targetTier: PlanTier,
        amount: Int,
        idempotencyKey: String,
        provider: PgProvider,
        billingKeyValue: String,
        maskedCard: String?
    ): CreatedOrder {
        val user = userRepository.getReferenceById(userId)
        val billingKey = billingKeyRepository.save(BillingKey(user, provider, billingKeyValue, maskedCard))
        val order = paymentOrderRepository.save(
            PaymentOrder(user, targetTier, amount, OrderType.INITIAL, idempotencyKey)
        )
        return CreatedOrder(order.id!!, order.publicId, billingKey.id!!)
    }

    // 청구 결과를 반영한다. 실패해도 이 메서드는 예외를 던지지 않고 실패 이력을 커밋한 뒤
    // ChargeApplyResult(success=false)를 반환한다 — 호출자가 트랜잭션 밖에서 PAY-002를 던진다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyInitialChargeResultInNewTransaction(
        userId: Long,
        orderId: Long,
        billingKeyId: Long,
        amount: Int,
        chargeResult: PaymentResult,
        now: LocalDateTime
    ): ChargeApplyResult {
        val order = paymentOrderRepository.getReferenceById(orderId)
        val payment: Payment = paymentRepository.save(Payment(order, amount, PaymentMethod.BILLING_KEY))

        if (!chargeResult.success || chargeResult.pgTransactionId == null) {
            payment.fail(chargeResult.failureCode, chargeResult.failureMessage)
            order.markFailed()
            return ChargeApplyResult(success = false, subscription = null)
        }

        payment.approve(chargeResult.pgTransactionId, chargeResult.approvedAt ?: now)
        order.markPaid()

        val billingKey = billingKeyRepository.getReferenceById(billingKeyId)
        val user = userRepository.getReferenceById(userId)
        val subscription = userSubscriptionRepository.findByUserId(userId)
            ?: userSubscriptionRepository.save(UserSubscription.createDefault(user))
        subscription.activatePaid(order.targetTier, now, now.plusMonths(1), billingKey)
        return ChargeApplyResult(success = true, subscription = subscription)
    }
}
