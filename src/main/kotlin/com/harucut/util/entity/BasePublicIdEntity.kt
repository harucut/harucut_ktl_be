package com.harucut.util.entity

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist

@MappedSuperclass
abstract class BasePublicIdEntity : BaseEntity() {

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String? = null
        protected set

    @PrePersist
    fun generatePublicId() {
        if (publicId == null) {
            publicId = NanoIdUtils.randomNanoId(
                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
                NanoIdUtils.DEFAULT_ALPHABET,
                10,
            )
        }
        onPrePersist()
    }

    protected open fun onPrePersist() {}
}