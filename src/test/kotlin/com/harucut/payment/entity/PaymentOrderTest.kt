package com.harucut.payment.entity

import com.harucut.payment.enums.OrderStatus
import com.harucut.payment.enums.OrderType
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentOrderTest {

    private fun user(): User = mockk(relaxed = true)

    private fun order(): PaymentOrder =
        PaymentOrder(user(), PlanTier.PRO, 9900, OrderType.INITIAL, "idem-key")

    @Nested
    inner class Create {

        @Test
        @DisplayName("생성 시 CREATED 상태로 시작한다")
        fun success() {
            assertThat(order().status).isEqualTo(OrderStatus.CREATED)
        }
    }

    @Nested
    inner class MarkPaid {

        @Test
        @DisplayName("PAID 상태로 전이한다")
        fun success() {
            val order = order()

            order.markPaid()

            assertThat(order.status).isEqualTo(OrderStatus.PAID)
        }
    }

    @Nested
    inner class MarkFailed {

        @Test
        @DisplayName("FAILED 상태로 전이한다")
        fun success() {
            val order = order()

            order.markFailed()

            assertThat(order.status).isEqualTo(OrderStatus.FAILED)
        }
    }
}
