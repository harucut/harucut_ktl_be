package com.harucut.media.repository

import com.harucut.media.entity.UserMedia
import com.harucut.media.enums.UserMediaType
import com.harucut.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserMediaRepository : JpaRepository<UserMedia, Long> {

    fun findByS3Key(s3Key: String): UserMedia?

    fun findByIdAndUser(id: Long, user: User): UserMedia?

    fun findAllByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<UserMedia>

    fun findAllByUserAndMediaTypeOrderByCreatedAtDesc(
        user: User,
        mediaType: UserMediaType,
        pageable: Pageable
    ): Page<UserMedia>

    fun findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        user: User,
        cutoff: LocalDateTime,
        pageable: Pageable
    ): Page<UserMedia>

    fun findAllByUserAndMediaTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        user: User,
        mediaType: UserMediaType,
        cutoff: LocalDateTime,
        pageable: Pageable
    ): Page<UserMedia>

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserMedia um WHERE um.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)
}