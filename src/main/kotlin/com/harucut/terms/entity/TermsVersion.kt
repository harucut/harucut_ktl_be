package com.harucut.terms.entity

import com.harucut.util.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "terms_version",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_terms_version_terms_id_version", columnNames = ["terms_id", "version"])
    ]
)
class TermsVersion(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    val terms: Terms,

    @Column(nullable = false)
    val version: Int,

    @Lob
    @Column(nullable = false)
    val content: String
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_version_id")
    val id: Long? = null
}
