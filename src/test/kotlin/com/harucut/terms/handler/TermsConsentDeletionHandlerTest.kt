package com.harucut.terms.handler

import com.harucut.terms.repository.TermsConsentRepository
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TermsConsentDeletionHandlerTest {

    private val termsConsentRepository = mockk<TermsConsentRepository>()
    private val handler = TermsConsentDeletionHandler(termsConsentRepository)

    @Test
    @DisplayName("탈퇴 시 해당 사용자의 약관 동의 이력을 삭제한다")
    fun deletesConsentHistory() {
        justRun { termsConsentRepository.deleteByUserId(7L) }

        handler.handleUserDeletion(7L)

        verify { termsConsentRepository.deleteByUserId(7L) }
    }
}
