package com.harucut.user.repository

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByProviderAndProviderId(provider: Provider, providerId: String): User?
    fun findByEmail(email: String): User?
}