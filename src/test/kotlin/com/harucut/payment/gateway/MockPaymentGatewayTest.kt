package com.harucut.payment.gateway

import com.harucut.payment.config.PaymentProperties
import com.harucut.payment.enums.PgProvider
import com.harucut.payment.gateway.dto.BillingChargeCommand
import com.harucut.payment.gateway.dto.IssueBillingKeyCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock

class MockPaymentGatewayTest {

    private val clock = Clock.systemDefaultZone()

    private fun gateway(failCharge: Boolean = false) =
        MockPaymentGateway(PaymentProperties(mock = PaymentProperties.Mock(failCharge = failCharge)), clock)

    @Nested
    inner class Provider {

        @Test
        @DisplayName("MOCK을 반환한다")
        fun success() {
            assertThat(gateway().provider()).isEqualTo(PgProvider.MOCK)
        }
    }

    @Nested
    inner class IssueBillingKey {

        @Test
        @DisplayName("정상 요청이면 빌링키를 발급한다")
        fun success() {
            val result = gateway().issueBillingKey(IssueBillingKeyCommand("customer-1", "auth-1"))

            assertThat(result.success).isTrue()
            assertThat(result.billingKeyValue).isNotBlank()
        }

        @Test
        @DisplayName("authKey에 FAIL이 포함되면 발급에 실패한다")
        fun fail() {
            val result = gateway().issueBillingKey(IssueBillingKeyCommand("customer-1", "FAIL-auth"))

            assertThat(result.success).isFalse()
            assertThat(result.billingKeyValue).isNull()
            assertThat(result.failureCode).isNotBlank()
        }
    }

    @Nested
    inner class Charge {

        @Test
        @DisplayName("정상 요청이면 청구에 성공한다")
        fun success() {
            val result = gateway().charge(
                BillingChargeCommand("bk-value", "order-1", 9900, "PRO 구독", "customer-1")
            )

            assertThat(result.success).isTrue()
            assertThat(result.pgTransactionId).isNotBlank()
            assertThat(result.approvedAt).isNotNull()
        }

        @Test
        @DisplayName("빌링키에 FAIL이 포함되면 청구에 실패한다")
        fun failBySentinel() {
            val result = gateway().charge(
                BillingChargeCommand("FAIL-bk", "order-1", 9900, "PRO 구독", "customer-1")
            )

            assertThat(result.success).isFalse()
        }

        @Test
        @DisplayName("payment.mock.fail-charge=true 이면 항상 청구에 실패한다")
        fun failByProperty() {
            val result = gateway(failCharge = true).charge(
                BillingChargeCommand("bk-value", "order-1", 9900, "PRO 구독", "customer-1")
            )

            assertThat(result.success).isFalse()
        }
    }
}
