package com.harucut.media.repository

import com.harucut.media.entity.UserMedia
import com.harucut.media.enums.UserMediaType
import com.harucut.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserMediaRepository : JpaRepository<UserMedia, Long> {

    fun findAllByUserOrderByCreatedAtDesc(user: User): List<UserMedia>

    fun findAllByUserAndMediaTypeOrderByCreatedAtDesc(
        user: User, mediaType: UserMediaType
    ): List<UserMedia>

    fun findAllByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<UserMedia>

    fun findAllByUserAndMediaTypeOrderByCreatedAtDesc(
        user: User, mediaType: UserMediaType, pageable: Pageable
    ): Page<UserMedia>

    fun findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        user: User, cutoff: LocalDateTime, pageable: Pageable
    ): Page<UserMedia>

    fun findAllByUserAndMediaTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        user: User, mediaType: UserMediaType, cutoff: LocalDateTime, pageable: Pageable
    ): Page<UserMedia>

    fun findByIdAndUser(mediaId: Long, user: User): UserMedia?

    fun existsByS3Key(s3Key: String): Boolean

    fun findByS3Key(s3Key: String): UserMedia?

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserMedia um WHERE um.user.id = :userId")
    fun deleteByUserId(userId: Long)
}
