package com.harucut.terms.entity

import com.harucut.util.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "terms",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_terms_code", columnNames = ["code"])
    ]
)
class Terms(
    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(nullable = false)
    val required: Boolean
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_id")
    val id: Long? = null

    @Column(nullable = false)
    var active: Boolean = true
        protected set

    // 약관 비활성화 (멱등)
    fun deactivate() {
        this.active = false
    }
}
