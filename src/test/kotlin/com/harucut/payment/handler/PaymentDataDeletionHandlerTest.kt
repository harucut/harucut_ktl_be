package com.harucut.payment.handler

import com.harucut.payment.repository.BillingKeyRepository
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PaymentDataDeletionHandlerTest {

    private val billingKeyRepository = mockk<BillingKeyRepository>()
    private val handler = PaymentDataDeletionHandler(billingKeyRepository)

    @Test
    @DisplayName("탈퇴 시 해당 사용자의 빌링키를 삭제한다")
    fun deletesBillingKey() {
        justRun { billingKeyRepository.deleteByUserId(7L) }

        handler.handleUserDeletion(7L)

        verify { billingKeyRepository.deleteByUserId(7L) }
    }
}
