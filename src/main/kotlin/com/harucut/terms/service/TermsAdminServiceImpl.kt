package com.harucut.terms.service

import com.harucut.exception.BusinessException
import com.harucut.terms.dto.TermsAdminResponse
import com.harucut.terms.entity.Terms
import com.harucut.terms.entity.TermsVersion
import com.harucut.terms.exception.TermsErrorCode
import com.harucut.terms.repository.TermsRepository
import com.harucut.terms.repository.TermsVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TermsAdminServiceImpl(
    private val termsRepository: TermsRepository,
    private val termsVersionRepository: TermsVersionRepository
) : TermsAdminService {

    // 약관 생성 (terms + version 1)
    override fun createTerms(code: String, title: String, required: Boolean, content: String) {
        if (termsRepository.existsByCode(code)) {
            throw BusinessException(TermsErrorCode.TERMS_CODE_DUPLICATED)
        }

        val terms = termsRepository.save(Terms(code = code, title = title, required = required))
        termsVersionRepository.save(TermsVersion(terms = terms, version = 1, content = content))
    }

    // 약관 개정 (version = max + 1)
    override fun reviseTerms(termsId: Long, content: String) {
        val terms = getTermsById(termsId)
        val nextVersion = (termsVersionRepository.findFirstByTermsIdOrderByVersionDesc(termsId)?.version ?: 0) + 1

        termsVersionRepository.save(TermsVersion(terms = terms, version = nextVersion, content = content))
    }

    // 전체 약관 목록 (비활성 포함, 현재 본문 + 최신 버전 번호)
    @Transactional(readOnly = true)
    override fun listAllTerms(): List<TermsAdminResponse> =
        termsRepository.findAll().map { terms ->
            val latest = termsVersionRepository.findFirstByTermsIdOrderByVersionDesc(terms.id!!)
                ?: throw BusinessException(TermsErrorCode.TERMS_NOT_FOUND)
            TermsAdminResponse.from(terms, latest)
        }

    // 약관 비활성화 (멱등)
    override fun deactivateTerms(termsId: Long) {
        getTermsById(termsId).deactivate()
    }

    private fun getTermsById(termsId: Long): Terms =
        termsRepository.findById(termsId).orElseThrow { BusinessException(TermsErrorCode.TERMS_NOT_FOUND) }
}
