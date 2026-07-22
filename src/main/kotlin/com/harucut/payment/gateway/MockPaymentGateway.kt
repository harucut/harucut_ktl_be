package com.harucut.payment.gateway

import com.harucut.payment.config.PaymentProperties
import com.harucut.payment.enums.PgProvider
import com.harucut.payment.gateway.dto.BillingChargeCommand
import com.harucut.payment.gateway.dto.BillingKeyResult
import com.harucut.payment.gateway.dto.CancelCommand
import com.harucut.payment.gateway.dto.CancelResult
import com.harucut.payment.gateway.dto.IssueBillingKeyCommand
import com.harucut.payment.gateway.dto.PaymentResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

// 실 PG 연동 전 기본 게이트웨이. HTTP 호출 없이 결정론적으로 응답한다.
// 실패 주입: authKey/billingKeyValue에 "FAIL" 포함 또는 payment.mock.fail-charge=true.
@Component
@ConditionalOnProperty(prefix = "payment.gateway", name = ["provider"], havingValue = "mock", matchIfMissing = true)
class MockPaymentGateway(
    private val paymentProperties: PaymentProperties,
    private val clock: Clock
) : PaymentGateway {

    override fun provider(): PgProvider = PgProvider.MOCK

    override fun issueBillingKey(command: IssueBillingKeyCommand): BillingKeyResult {
        if (command.authKey.contains("FAIL")) {
            return BillingKeyResult(
                success = false,
                billingKeyValue = null,
                maskedCard = null,
                failureCode = "MOCK_ISSUE_FAILED",
                failureMessage = "Mock billing key issuance failed."
            )
        }
        return BillingKeyResult(
            success = true,
            billingKeyValue = "mock-bk-${command.customerKey}-${System.nanoTime()}",
            maskedCard = "**** **** **** 1234",
            failureCode = null,
            failureMessage = null
        )
    }

    override fun charge(command: BillingChargeCommand): PaymentResult {
        if (paymentProperties.mock.failCharge || command.billingKeyValue.contains("FAIL")) {
            return PaymentResult(
                success = false,
                pgTransactionId = null,
                approvedAt = null,
                failureCode = "MOCK_CHARGE_FAILED",
                failureMessage = "Mock charge failed."
            )
        }
        return PaymentResult(
            success = true,
            pgTransactionId = "mock-tx-${command.orderKey}",
            approvedAt = LocalDateTime.now(clock),
            failureCode = null,
            failureMessage = null
        )
    }

    override fun inquire(pgTransactionId: String): PaymentResult =
        PaymentResult(
            success = true,
            pgTransactionId = pgTransactionId,
            approvedAt = LocalDateTime.now(clock),
            failureCode = null,
            failureMessage = null
        )

    override fun cancel(command: CancelCommand): CancelResult =
        CancelResult(success = true, failureCode = null, failureMessage = null)
}
