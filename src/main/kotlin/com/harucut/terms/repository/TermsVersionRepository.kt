package com.harucut.terms.repository

import com.harucut.terms.entity.TermsVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TermsVersionRepository : JpaRepository<TermsVersion, Long> {

    // 활성 약관의 코드별 최신 버전 (본문 포함)
    @Query(
        """
        SELECT tv FROM TermsVersion tv
        JOIN FETCH tv.terms t
        WHERE t.active = true
        AND tv.version = (SELECT MAX(v.version) FROM TermsVersion v WHERE v.terms = tv.terms)
        """
    )
    fun findLatestActiveVersions(): List<TermsVersion>

    fun findFirstByTermsIdOrderByVersionDesc(termsId: Long): TermsVersion?
}
