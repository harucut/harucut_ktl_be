package com.harucut.terms.service

import com.harucut.terms.dto.ConsentItem
import com.harucut.terms.dto.TermsConsentStatusResponse
import com.harucut.terms.dto.TermsResponse

interface TermsService {
    fun getActiveTerms(): List<TermsResponse>
    fun getMyConsentStatus(userId: Long): List<TermsConsentStatusResponse>
    fun consent(userId: Long, items: List<ConsentItem>)

    // 내부용: 미래 광고메일 등 발송 로직이 참조할 동의 여부 단일 소스
    fun hasActiveConsent(userId: Long, code: String): Boolean
}
