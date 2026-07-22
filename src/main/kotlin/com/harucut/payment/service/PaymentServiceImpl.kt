package com.harucut.payment.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.payment.exception.PaymentErrorCode
import com.harucut.payment.gateway.PaymentGateway
import com.harucut.payment.gateway.dto.BillingChargeCommand
import com.harucut.payment.gateway.dto.IssueBillingKeyCommand
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.config.PlanPricingProperties
import com.harucut.user.repository.UserRepository
import com.harucut.util.component.generatePublicId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PaymentServiceImpl(
    private val userRepository: UserRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val paymentTransactionService: PaymentTransactionService,
    private val paymentGateway: PaymentGateway,
    private val planPricingProperties: PlanPricingProperties,
    private val clock: Clock
) : PaymentService {

    override fun subscribe(userId: Long, planTier: PlanTier, customerKey: String, authKey: String): SubscriptionResponse {
        if (planTier == PlanTier.BASIC) {
            throw BusinessException(PaymentErrorCode.INVALID_TARGET_PLAN)
        }
        if (!userRepository.existsById(userId)) {
            throw BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.")
        }

        val now = LocalDateTime.now(clock)
        val existing = userSubscriptionRepository.findByUserId(userId)
        if (existing != null && existing.effectiveTier(now) != PlanTier.BASIC) {
            throw BusinessException(PaymentErrorCode.ALREADY_SUBSCRIBED)
        }

        val billingKeyResult = paymentGateway.issueBillingKey(IssueBillingKeyCommand(customerKey, authKey))
        if (!billingKeyResult.success || billingKeyResult.billingKeyValue == null) {
            throw BusinessException(PaymentErrorCode.BILLING_KEY_ISSUE_FAILED)
        }

        val amount = planPricingProperties.priceOf(planTier)
        val idempotencyKey = "initial:$userId:${generatePublicId()}"
        val created = paymentTransactionService.createInitialOrderInNewTransaction(
            userId,
            planTier,
            amount,
            idempotencyKey,
            paymentGateway.provider(),
            billingKeyResult.billingKeyValue,
            billingKeyResult.maskedCard
        )

        val chargeResult = paymentGateway.charge(
            BillingChargeCommand(
                billingKeyValue = billingKeyResult.billingKeyValue,
                orderKey = created.orderPublicId,
                amount = amount,
                orderName = "${planTier.name} 구독",
                customerKey = customerKey
            )
        )

        val applyResult = paymentTransactionService.applyInitialChargeResultInNewTransaction(
            userId, created.orderId, created.billingKeyId, amount, chargeResult, now
        )

        if (!applyResult.success || applyResult.subscription == null) {
            throw BusinessException(PaymentErrorCode.PAYMENT_FAILED)
        }

        return SubscriptionResponse.from(applyResult.subscription)
    }
}
