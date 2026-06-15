package com.harucut.user.repository

import com.harucut.auth.oauth2.enums.Provider
import com.harucut.user.entity.User
import com.harucut.user.enums.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun existsByProviderAndEmail(provider: Provider, email: String): Boolean

    fun findByProviderAndEmail(provider: Provider, email: String): User?

    fun findByProviderAndProviderId(provider: Provider, providerId: String): User?

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
        userStatus: UserStatus,
        cutoffDate: LocalDateTime
    ): List<Long>
}