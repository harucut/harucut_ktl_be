package com.harucut.payment.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class PaymentErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    BILLING_KEY_ISSUE_FAILED("PAY-001", HttpStatus.BAD_GATEWAY, "Failed to issue a billing key."),
    PAYMENT_FAILED("PAY-002", HttpStatus.PAYMENT_REQUIRED, "Payment failed."),
    ALREADY_SUBSCRIBED("PAY-003", HttpStatus.CONFLICT, "Already subscribed to a paid plan."),
    BILLING_KEY_NOT_FOUND("PAY-004", HttpStatus.NOT_FOUND, "Billing key not found."),
    ORDER_NOT_FOUND("PAY-005", HttpStatus.NOT_FOUND, "Payment order not found."),
    DUPLICATE_PAYMENT("PAY-006", HttpStatus.CONFLICT, "Duplicate payment request."),
    INVALID_TARGET_PLAN("PAY-007", HttpStatus.BAD_REQUEST, "Invalid target plan tier."),
    WEBHOOK_SIGNATURE_INVALID("PAY-008", HttpStatus.BAD_REQUEST, "Invalid webhook signature."),
    REFUND_FAILED("PAY-009", HttpStatus.BAD_GATEWAY, "Refund failed."),
    GATEWAY_ERROR("PAY-010", HttpStatus.BAD_GATEWAY, "Payment gateway error."),
}
