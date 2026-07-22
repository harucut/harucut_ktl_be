package com.harucut.payment.entity

import com.harucut.payment.enums.BillingKeyStatus
import com.harucut.payment.enums.PgProvider
import com.harucut.user.entity.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BillingKeyTest {

    private fun user(): User = mockk(relaxed = true)

    @Nested
    inner class Create {

        @Test
        @DisplayName("생성 시 ACTIVE 상태로 시작한다")
        fun success() {
            val billingKey = BillingKey(user(), PgProvider.MOCK, "bk-value")

            assertThat(billingKey.status).isEqualTo(BillingKeyStatus.ACTIVE)
        }
    }

    @Nested
    inner class Delete {

        @Test
        @DisplayName("폐기하면 DELETED 상태가 된다")
        fun success() {
            val billingKey = BillingKey(user(), PgProvider.MOCK, "bk-value")

            billingKey.delete()

            assertThat(billingKey.status).isEqualTo(BillingKeyStatus.DELETED)
        }
    }
}
