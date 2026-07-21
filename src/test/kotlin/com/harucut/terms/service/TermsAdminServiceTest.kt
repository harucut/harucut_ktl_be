package com.harucut.terms.service

import com.harucut.exception.BusinessException
import com.harucut.terms.entity.Terms
import com.harucut.terms.entity.TermsVersion
import com.harucut.terms.exception.TermsErrorCode
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
import java.util.Optional

class TermsAdminServiceTest {

    private val termsRepository = mockk<TermsRepository>()
    private val termsVersionRepository = mockk<TermsVersionRepository>()
    private val service = TermsAdminServiceImpl(termsRepository, termsVersionRepository)

    private fun terms(code: String = "tos", required: Boolean = true): Terms =
        Terms(code = code, title = "이용약관", required = required)

    @Nested
    inner class CreateTerms {

        @Test
        @DisplayName("중복되지 않은 코드면 약관과 버전 1을 생성한다")
        fun success() {
            every { termsRepository.existsByCode("tos") } returns false
            val termsSlot = slot<Terms>()
            every { termsRepository.save(capture(termsSlot)) } answers { termsSlot.captured }
            val versionSlot = slot<TermsVersion>()
            every { termsVersionRepository.save(capture(versionSlot)) } answers { versionSlot.captured }

            service.createTerms("tos", "이용약관", true, "본문")

            assertThat(termsSlot.captured.code).isEqualTo("tos")
            assertThat(termsSlot.captured.title).isEqualTo("이용약관")
            assertThat(termsSlot.captured.required).isTrue()
            assertThat(versionSlot.captured.version).isEqualTo(1)
            assertThat(versionSlot.captured.content).isEqualTo("본문")
        }

        @Test
        @DisplayName("중복된 코드면 TERMS_CODE_DUPLICATED 예외를 던진다")
        fun duplicated() {
            every { termsRepository.existsByCode("tos") } returns true

            assertThatThrownBy { service.createTerms("tos", "이용약관", true, "본문") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(TermsErrorCode.TERMS_CODE_DUPLICATED)

            verify(exactly = 0) { termsRepository.save(any()) }
        }
    }

    @Nested
    inner class ReviseTerms {

        @Test
        @DisplayName("기존 최신 버전 + 1로 새 버전을 생성한다")
        fun success() {
            val t = terms()
            every { termsRepository.findById(1L) } returns Optional.of(t)
            every { termsVersionRepository.findFirstByTermsIdOrderByVersionDesc(1L) } returns
                    TermsVersion(terms = t, version = 2, content = "v2")
            val versionSlot = slot<TermsVersion>()
            every { termsVersionRepository.save(capture(versionSlot)) } answers { versionSlot.captured }

            service.reviseTerms(1L, "v3 본문")

            assertThat(versionSlot.captured.version).isEqualTo(3)
            assertThat(versionSlot.captured.content).isEqualTo("v3 본문")
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 TERMS_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { termsRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.reviseTerms(1L, "본문") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(TermsErrorCode.TERMS_NOT_FOUND)
        }
    }

    @Nested
    inner class DeactivateTerms {

        @Test
        @DisplayName("존재하는 약관을 비활성화한다")
        fun success() {
            val t = terms()
            every { termsRepository.findById(1L) } returns Optional.of(t)

            service.deactivateTerms(1L)

            assertThat(t.active).isFalse()
        }

        @Test
        @DisplayName("이미 비활성 상태여도 멱등하게 처리한다")
        fun idempotent() {
            val t = terms()
            t.deactivate()
            every { termsRepository.findById(1L) } returns Optional.of(t)

            service.deactivateTerms(1L)

            assertThat(t.active).isFalse()
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 TERMS_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { termsRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.deactivateTerms(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(TermsErrorCode.TERMS_NOT_FOUND)
        }
    }
}
