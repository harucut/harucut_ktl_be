package com.harucut.terms.service

import com.harucut.terms.dto.TermsAdminResponse

interface TermsAdminService {
    fun createTerms(code: String, title: String, required: Boolean, content: String)
    fun reviseTerms(termsId: Long, content: String)
    fun listAllTerms(): List<TermsAdminResponse>
    fun deactivateTerms(termsId: Long)
}
