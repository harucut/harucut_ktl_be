package com.harucut.terms.repository

import com.harucut.terms.entity.TermsConsent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TermsConsentRepository : JpaRepository<TermsConsent, Long> {

    // 사용자의 전체 동의 이력 (약관별 최신 판정용 - 사용자당 소량이라 한 번에 fetch)
    @Query(
        """
        SELECT tc FROM TermsConsent tc
        JOIN FETCH tc.termsVersion tv
        JOIN FETCH tv.terms t
        WHERE tc.userId = :userId
        ORDER BY tc.createdAt DESC
        """
    )
    fun findAllByUserIdOrderByCreatedAtDesc(@Param("userId") userId: Long): List<TermsConsent>

    // 특정 약관 코드에 대한 사용자의 최신 동의 행 (hasActiveConsent용)
    fun findFirstByUserIdAndTermsVersion_Terms_CodeOrderByCreatedAtDesc(userId: Long, code: String): TermsConsent?

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TermsConsent tc WHERE tc.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)
}
