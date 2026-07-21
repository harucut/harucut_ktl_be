package com.harucut.terms.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.terms.repository.TermsConsentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TermsConsentDeletionHandler(
    private val termsConsentRepository: TermsConsentRepository
) : UserDeletionHandler {

    // 탈퇴 하드삭제 시 사용자 약관 동의 이력 삭제
    @Transactional
    override fun handleUserDeletion(userId: Long) {
        termsConsentRepository.deleteByUserId(userId)
    }
}
