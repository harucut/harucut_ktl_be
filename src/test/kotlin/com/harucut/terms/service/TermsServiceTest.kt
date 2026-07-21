package com.harucut.terms.service

import com.harucut.exception.BusinessException
import com.harucut.terms.dto.ConsentItem
import com.harucut.terms.entity.Terms
import com.harucut.terms.entity.TermsConsent
import com.harucut.terms.entity.TermsVersion
import com.harucut.terms.enums.TermsConsentStatus
import com.harucut.terms.exception.TermsErrorCode
import com.harucut.terms.repository.TermsConsentRepository
import com.harucut.terms.repository.TermsRepository
import com.harucut.terms.repository.TermsVersionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TermsServiceTest {

    private val termsRepository = mockk<TermsRepository>()
    private val termsVersionRepository = mockk<TermsVersionRepository>()
    private val termsConsentRepository = mockk<TermsConsentRepository>()
    private val service = TermsServiceImpl(termsRepository, termsVersionRepository, termsConsentRepository)

    private fun terms(code: String, required: Boolean = true, id: Long = 1L): Terms =
        mockk<Terms>(relaxed = true).also {
            every { it.id } returns id
            every { it.code } returns code
            every { it.title } returns "제목"
            every { it.required } returns required
        }

    private fun version(t: Terms, version: Int, content: String = "본문"): TermsVersion =
        TermsVersion(terms = t, version = version, content = content)

    private fun consent(userId: Long, v: TermsVersion, agreed: Boolean): TermsConsent =
        TermsConsent(userId = userId, termsVersion = v, agreed = agreed)

    @Nested
    inner class GetActiveTerms {

        @Test
        @DisplayName("활성 약관의 최신 버전을 응답으로 변환한다")
        fun success() {
            val t = terms("tos")
            every { termsVersionRepository.findLatestActiveVersions() } returns listOf(version(t, 1, "본문1"))

            val result = service.getActiveTerms()

            assertThat(result).hasSize(1)
            assertThat(result[0].code).isEqualTo("tos")
            assertThat(result[0].version).isEqualTo(1)
            assertThat(result[0].content).isEqualTo("본문1")
        }
    }

    @Nested
    inner class Consent {

        @Test
        @DisplayName("각 코드의 최신 버전을 참조하는 동의 행을 append한다")
        fun success() {
            val t = terms("tos", required = true)
            every { termsRepository.findByCodeAndActiveTrue("tos") } returns t
            every { termsVersionRepository.findFirstByTermsIdOrderByVersionDesc(any()) } returns version(t, 2)
            val consentSlot = slot<TermsConsent>()
            every { termsConsentRepository.save(capture(consentSlot)) } answers { consentSlot.captured }

            service.consent(1L, listOf(ConsentItem(code = "tos", agreed = true)))

            assertThat(consentSlot.captured.userId).isEqualTo(1L)
            assertThat(consentSlot.captured.termsVersion.version).isEqualTo(2)
            assertThat(consentSlot.captured.agreed).isTrue()
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화된 약관 코드면 TERMS_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { termsRepository.findByCodeAndActiveTrue("unknown") } returns null

            assertThatThrownBy { service.consent(1L, listOf(ConsentItem(code = "unknown", agreed = true))) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(TermsErrorCode.TERMS_NOT_FOUND)

            verify(exactly = 0) { termsConsentRepository.save(any()) }
        }

        @Test
        @DisplayName("필수 약관을 agreed=false로 철회하려 하면 REQUIRED_TERMS_CANNOT_WITHDRAW 예외를 던진다")
        fun requiredWithdraw() {
            val t = terms("tos", required = true)
            every { termsRepository.findByCodeAndActiveTrue("tos") } returns t

            assertThatThrownBy { service.consent(1L, listOf(ConsentItem(code = "tos", agreed = false))) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(TermsErrorCode.REQUIRED_TERMS_CANNOT_WITHDRAW)

            verify(exactly = 0) { termsConsentRepository.save(any()) }
        }

        @Test
        @DisplayName("선택 약관은 agreed=false로 철회할 수 있다")
        fun optionalWithdrawOk() {
            val t = terms("marketing", required = false)
            every { termsRepository.findByCodeAndActiveTrue("marketing") } returns t
            every { termsVersionRepository.findFirstByTermsIdOrderByVersionDesc(any()) } returns version(t, 1)
            every { termsConsentRepository.save(any()) } answers { firstArg() }

            service.consent(1L, listOf(ConsentItem(code = "marketing", agreed = false)))

            verify { termsConsentRepository.save(any()) }
        }
    }

    @Nested
    inner class GetMyConsentStatus {

        @Test
        @DisplayName("최신 버전에 동의했으면 AGREED를 반환한다")
        fun agreed() {
            val t = terms("tos")
            val v1 = version(t, 1)
            every { termsVersionRepository.findLatestActiveVersions() } returns listOf(v1)
            every { termsConsentRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns
                    listOf(consent(1L, v1, true))

            val result = service.getMyConsentStatus(1L)

            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(TermsConsentStatus.AGREED)
            assertThat(result[0].agreedVersion).isEqualTo(1)
            assertThat(result[0].latestVersion).isEqualTo(1)
        }

        @Test
        @DisplayName("구버전에 동의한 뒤 개정되었으면 NEEDS_RECONSENT를 반환한다")
        fun needsReconsent() {
            val t = terms("tos")
            val v1 = version(t, 1)
            val v2 = version(t, 2)
            every { termsVersionRepository.findLatestActiveVersions() } returns listOf(v2)
            every { termsConsentRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns
                    listOf(consent(1L, v1, true))

            val result = service.getMyConsentStatus(1L)

            assertThat(result[0].status).isEqualTo(TermsConsentStatus.NEEDS_RECONSENT)
            assertThat(result[0].agreedVersion).isEqualTo(1)
            assertThat(result[0].latestVersion).isEqualTo(2)
        }

        @Test
        @DisplayName("동의 이력이 없으면 NOT_AGREED를 반환한다")
        fun notAgreedNoHistory() {
            val t = terms("tos")
            val v1 = version(t, 1)
            every { termsVersionRepository.findLatestActiveVersions() } returns listOf(v1)
            every { termsConsentRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns emptyList()

            val result = service.getMyConsentStatus(1L)

            assertThat(result[0].status).isEqualTo(TermsConsentStatus.NOT_AGREED)
            assertThat(result[0].agreedVersion).isNull()
        }

        @Test
        @DisplayName("최신 이력이 철회(agreed=false)이면 NOT_AGREED를 반환한다")
        fun notAgreedWithdrawn() {
            val t = terms("marketing", required = false)
            val v1 = version(t, 1)
            every { termsVersionRepository.findLatestActiveVersions() } returns listOf(v1)
            every { termsConsentRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns
                    listOf(consent(1L, v1, false))

            val result = service.getMyConsentStatus(1L)

            assertThat(result[0].status).isEqualTo(TermsConsentStatus.NOT_AGREED)
            assertThat(result[0].agreedVersion).isNull()
        }
    }

    @Nested
    inner class HasActiveConsent {

        @Test
        @DisplayName("약관이 없거나 비활성이면 false를 반환한다")
        fun noTerms() {
            every { termsRepository.findByCodeAndActiveTrue("tos") } returns null

            assertThat(service.hasActiveConsent(1L, "tos")).isFalse()
        }

        @Test
        @DisplayName("동의 이력이 없으면 false를 반환한다")
        fun noConsent() {
            val t = terms("tos")
            every { termsRepository.findByCodeAndActiveTrue("tos") } returns t
            every {
                termsConsentRepository.findFirstByUserIdAndTermsVersion_Terms_CodeOrderByCreatedAtDesc(1L, "tos")
            } returns null

            assertThat(service.hasActiveConsent(1L, "tos")).isFalse()
        }

        @Test
        @DisplayName("최신 동의 행이 agreed=true이면 true를 반환한다")
        fun agreedTrue() {
            val t = terms("tos")
            val v1 = version(t, 1)
            every { termsRepository.findByCodeAndActiveTrue("tos") } returns t
            every {
                termsConsentRepository.findFirstByUserIdAndTermsVersion_Terms_CodeOrderByCreatedAtDesc(1L, "tos")
            } returns consent(1L, v1, true)

            assertThat(service.hasActiveConsent(1L, "tos")).isTrue()
        }

        @Test
        @DisplayName("최신 동의 행이 agreed=false(철회)이면 false를 반환한다")
        fun agreedFalse() {
            val t = terms("tos")
            val v1 = version(t, 1)
            every { termsRepository.findByCodeAndActiveTrue("tos") } returns t
            every {
                termsConsentRepository.findFirstByUserIdAndTermsVersion_Terms_CodeOrderByCreatedAtDesc(1L, "tos")
            } returns consent(1L, v1, false)

            assertThat(service.hasActiveConsent(1L, "tos")).isFalse()
        }
    }
}
