package com.harucut.user.repository

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserRepository : JpaRepository<User, Long> {

    fun findByProviderAndProviderId(provider: Provider, providerId: String): User?

    fun findByEmail(email: String): User?

    fun findByProviderAndEmail(provider: Provider, email: String): User?

    fun existsByProviderAndEmail(provider: Provider, email: String): Boolean

    fun findByPublicId(publicId: String): User?

    @Query(
        """
            select u.id
            from User u
            where u.userStatus = :userStatus
            and u.deleteRequestedAt <= :cutoffDate
        """
    )
    fun findExpiredDeleteRequestedUserIds(
        @Param("userStatus") userStatus: UserStatus,
        @Param("cutoffDate") cutoffDate: LocalDateTime
    ): List<Long>
}