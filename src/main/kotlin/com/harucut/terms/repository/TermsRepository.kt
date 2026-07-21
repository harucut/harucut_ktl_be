package com.harucut.terms.repository

import com.harucut.terms.entity.Terms
import org.springframework.data.jpa.repository.JpaRepository

interface TermsRepository : JpaRepository<Terms, Long> {

    fun existsByCode(code: String): Boolean

    fun findByCodeAndActiveTrue(code: String): Terms?
}
