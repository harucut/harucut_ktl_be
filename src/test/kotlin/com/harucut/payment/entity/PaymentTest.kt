package com.harucut.payment.entity

import com.harucut.payment.enums.PaymentMethod
import com.harucut.payment.enums.PaymentStatus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PaymentTest {

    private fun order(): PaymentOrder = mockk(relaxed = true)

    private fun payment(): Payment = Payment(order(), 9900, PaymentMethod.BILLING_KEY)

    @Nested
    inner class Create {

        @Test
        @DisplayName("생성 시 REQUESTED 상태로 시작한다")
        fun success() {
            assertThat(payment().status).isEqualTo(PaymentStatus.REQUESTED)
        }
    }

    @Nested
    inner class Approve {

        @Test
        @DisplayName("승인 시 APPROVED 상태와 PG 거래ID·승인시각을 기록한다")
        fun success() {
            val payment = payment()
            val approvedAt = LocalDateTime.now()

            payment.approve("tx-123", approvedAt)

            assertThat(payment.status).isEqualTo(PaymentStatus.APPROVED)
            assertThat(payment.pgTransactionId).isEqualTo("tx-123")
            assertThat(payment.approvedAt).isEqualTo(approvedAt)
        }
    }

    @Nested
    inner class Fail {

        @Test
        @DisplayName("실패 시 FAILED 상태와 실패 코드·메시지를 기록한다")
        fun success() {
            val payment = payment()

            payment.fail("CARD_DECLINED", "카드 승인 거절")

            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.failureCode).isEqualTo("CARD_DECLINED")
            assertThat(payment.failureMessage).isEqualTo("카드 승인 거절")
        }
    }
}
