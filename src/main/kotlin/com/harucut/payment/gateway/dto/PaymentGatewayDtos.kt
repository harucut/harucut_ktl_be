package com.harucut.payment.gateway.dto

import java.time.LocalDateTime

data class IssueBillingKeyCommand(
    val customerKey: String,
    val authKey: String
)

data class BillingKeyResult(
    val success: Boolean,
    val billingKeyValue: String?,
    val maskedCard: String?,
    val failureCode: String?,
    val failureMessage: String?
)

data class BillingChargeCommand(
    val billingKeyValue: String,
    val orderKey: String,
    val amount: Int,
    val orderName: String,
    val customerKey: String
)

data class PaymentResult(
    val success: Boolean,
    val pgTransactionId: String?,
    val approvedAt: LocalDateTime?,
    val failureCode: String?,
    val failureMessage: String?
)

data class CancelCommand(
    val pgTransactionId: String,
    val reason: String
)

data class CancelResult(
    val success: Boolean,
    val failureCode: String?,
    val failureMessage: String?
)
