package com.harucut.frame.repository

import com.harucut.frame.entity.Frame
import com.harucut.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FrameRepository : JpaRepository<Frame, Long> {

    fun findAllByUser(user: User): List<Frame>

    fun countByUserId(userId: Long): Long

    fun findByIdAndUser(frameId: Long, user: User): Frame?

    @Modifying(clearAutomatically = true)
    @Query(
        """
        DELETE FROM FrameComponent fc
        WHERE fc.frame.id IN (SELECT f.id FROM Frame f WHERE f.user.id = :userId)
    """
    )
    fun deleteComponentsByUserId(userId: Long)

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Frame f WHERE f.user.id = :userId")
    fun deleteFramesByUserId(userId: Long)
}