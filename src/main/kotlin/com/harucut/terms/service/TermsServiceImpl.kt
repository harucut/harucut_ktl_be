package com.harucut.terms.service

import com.harucut.exception.BusinessException
import com.harucut.terms.dto.ConsentItem
import com.harucut.terms.dto.TermsConsentStatusResponse
import com.harucut.terms.dto.TermsResponse
import com.harucut.terms.entity.TermsConsent
import com.harucut.terms.enums.TermsConsentStatus
import com.harucut.terms.exception.TermsErrorCode
import com.harucut.terms.repository.TermsConsentRepository
import com.harucut.terms.repository.TermsRepository
import com.harucut.terms.repository.TermsVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TermsServiceImpl(
    private val termsRepository: TermsRepository,
    private val termsVersionRepository: TermsVersionRepository,
    private val termsConsentRepository: TermsConsentRepository
) : TermsService {

    // 활성 약관의 코드별 최신 버전 (본문 포함)
    @Transactional(readOnly = true)
    override fun getActiveTerms(): List<TermsResponse> =
        termsVersionRepository.findLatestActiveVersions().map { TermsResponse.from(it) }

    // 내 동의 상태: 활성 약관별로 이력상 가장 최신 동의 행과 비교해 판정
    @Transactional(readOnly = true)
    override fun getMyConsentStatus(userId: Long): List<TermsConsentStatusResponse> {
        val latestVersions = termsVersionRepository.findLatestActiveVersions()
        val latestConsentByCode = termsConsentRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .groupBy { it.termsVersion.terms.code }
            .mapValues { it.value.first() }

        return latestVersions.map { latestVersion ->
            val terms = latestVersion.terms
            val consent = latestConsentByCode[terms.code]
            val status = when {
                consent == null || !consent.agreed -> TermsConsentStatus.NOT_AGREED
                consent.termsVersion.version == latestVersion.version -> TermsConsentStatus.AGREED
                else -> TermsConsentStatus.NEEDS_RECONSENT
            }

            TermsConsentStatusResponse(
                code = terms.code,
                title = terms.title,
                required = terms.required,
                status = status,
                agreedVersion = consent?.takeIf { it.agreed }?.termsVersion?.version,
                latestVersion = latestVersion.version
            )
        }
    }

    // 동의 + 철회 통합 처리: 각 코드 최신 버전을 참조하는 이력 행 append
    override fun consent(userId: Long, items: List<ConsentItem>) {
        items.forEach { item ->
            val terms = termsRepository.findByCodeAndActiveTrue(item.code)
                ?: throw BusinessException(TermsErrorCode.TERMS_NOT_FOUND)

            if (!item.agreed && terms.required) {
                throw BusinessException(TermsErrorCode.REQUIRED_TERMS_CANNOT_WITHDRAW)
            }

            val latestVersion = termsVersionRepository.findFirstByTermsIdOrderByVersionDesc(terms.id!!)
                ?: throw BusinessException(TermsErrorCode.TERMS_NOT_FOUND)

            termsConsentRepository.save(
                TermsConsent(userId = userId, termsVersion = latestVersion, agreed = item.agreed)
            )
        }
    }

    @Transactional(readOnly = true)
    override fun hasActiveConsent(userId: Long, code: String): Boolean {
        termsRepository.findByCodeAndActiveTrue(code) ?: return false

        val latestConsent = termsConsentRepository
            .findFirstByUserIdAndTermsVersion_Terms_CodeOrderByCreatedAtDesc(userId, code)

        return latestConsent?.agreed == true
    }
}
