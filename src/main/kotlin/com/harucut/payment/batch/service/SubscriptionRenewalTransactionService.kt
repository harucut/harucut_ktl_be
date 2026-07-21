package com.harucut.payment.batch.service

import com.harucut.payment.entity.Payment
import com.harucut.payment.enums.OrderType
import com.harucut.payment.enums.PaymentMethod
import com.harucut.payment.entity.PaymentOrder
import com.harucut.payment.gateway.dto.PaymentResult
import com.harucut.payment.repository.PaymentOrderRepository
import com.harucut.payment.repository.PaymentRepository
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.config.PlanPricingProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// PG 외부 호출 전후로 DB 커밋 경계를 분리하기 위한 헬퍼(PaymentTransactionService와 동일한 목적).
// "PG 성공 + DB 기록 실패"로 결제/멱등키 이력이 유실(→ 다음날 이중청구)되는 상황을 막기 위해
// 주문 생성과 결제 결과 반영을 각각 독립 트랜잭션(REQUIRES_NEW)으로 커밋한다.
@Service
class SubscriptionRenewalTransactionService(
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentRepository: PaymentRepository,
    private val planPricingProperties: PlanPricingProperties
) {

    sealed class RenewalPreparation {
        object NotFound : RenewalPreparation()
        object NoBillingKey : RenewalPreparation()
        object AlreadyProcessed : RenewalPreparation()
        data class Created(
            val orderId: Long,
            val orderPublicId: String,
            val billingKeyValue: String,
            val userPublicId: String,
            val planTier: PlanTier,
            val amount: Int
        ) : RenewalPreparation()
    }

    // 구독 조회 + 빌링키 확인 + 멱등 주문 생성을 먼저 커밋한다(청구 호출 이전에 반드시 커밋되어야 함).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun prepareRenewalOrderInNewTransaction(subscriptionId: Long, now: LocalDateTime): RenewalPreparation {
        val subscription = userSubscriptionRepository.findById(subscriptionId).orElse(null)
            ?: return RenewalPreparation.NotFound

        val billingKey = subscription.billingKey ?: run {
            subscription.markPastDue()
            return RenewalPreparation.NoBillingKey
        }

        val idempotencyKey = "renewal:$subscriptionId:${now.format(YEAR_MONTH_FORMAT)}"
        if (paymentOrderRepository.existsByIdempotencyKey(idempotencyKey)) {
            // 같은 결제 주기에 이미 처리됨 — 중복 청구 방지(멱등)
            return RenewalPreparation.AlreadyProcessed
        }

        val amount = planPricingProperties.priceOf(subscription.planTier)
        val order: PaymentOrder = paymentOrderRepository.save(
            PaymentOrder(subscription.user, subscription.planTier, amount, OrderType.RENEWAL, idempotencyKey)
        )

        return RenewalPreparation.Created(
            orderId = order.id!!,
            orderPublicId = order.publicId,
            billingKeyValue = billingKey.billingKeyValue,
            userPublicId = subscription.user.publicId,
            planTier = subscription.planTier,
            amount = amount
        )
    }

    // 청구 결과를 반영한다. 성공/실패 어느 쪽이든 이 트랜잭션은 그대로 커밋되어 결제 이력이 보존된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyRenewalChargeResultInNewTransaction(
        subscriptionId: Long,
        orderId: Long,
        amount: Int,
        chargeResult: PaymentResult,
        now: LocalDateTime
    ) {
        val order = paymentOrderRepository.getReferenceById(orderId)
        val payment: Payment = paymentRepository.save(Payment(order, amount, PaymentMethod.BILLING_KEY))
        val subscription = userSubscriptionRepository.getReferenceById(subscriptionId)

        if (chargeResult.success && chargeResult.pgTransactionId != null) {
            payment.approve(chargeResult.pgTransactionId, chargeResult.approvedAt ?: now)
            order.markPaid()
            subscription.renew(now, now.plusMonths(1))
        } else {
            payment.fail(chargeResult.failureCode, chargeResult.failureMessage)
            order.markFailed()
            subscription.markPastDue()
        }
    }

    companion object {
        private val YEAR_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")
    }
}
