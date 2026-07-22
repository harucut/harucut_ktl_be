package com.harucut.payment.gateway

import com.harucut.payment.enums.PgProvider
import com.harucut.payment.gateway.dto.BillingChargeCommand
import com.harucut.payment.gateway.dto.BillingKeyResult
import com.harucut.payment.gateway.dto.CancelCommand
import com.harucut.payment.gateway.dto.CancelResult
import com.harucut.payment.gateway.dto.IssueBillingKeyCommand
import com.harucut.payment.gateway.dto.PaymentResult

// PG사에 묶이지 않는 결제 게이트웨이 추상화. 실 PG 연동 전까지는 MockPaymentGateway만 존재.
interface PaymentGateway {

    fun provider(): PgProvider

    fun issueBillingKey(command: IssueBillingKeyCommand): BillingKeyResult

    fun charge(command: BillingChargeCommand): PaymentResult

    fun inquire(pgTransactionId: String): PaymentResult

    fun cancel(command: CancelCommand): CancelResult
}
