package com.harucut.frame.repository

import com.harucut.frame.entity.Frame
import com.harucut.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FrameRepository : JpaRepository<Frame, Long> {

    // 사용자 프레임 전체 조회 (최신순)
    fun findAllByUserOrderByCreatedAtDesc(user: User): List<Frame>

    // 사용자가 현재 보관 중인 프레임 개수 (동시 보관 cap 판정용)
    fun countByUser(user: User): Long

    // 탈퇴 하드삭제: 사용자 프레임의 컴포넌트 일괄 삭제 (FK 때문에 프레임보다 먼저)
    @Modifying(clearAutomatically = true)
    @Query(
        """
        DELETE FROM FrameComponent fc
        WHERE fc.frame.id IN (SELECT f.id FROM Frame f WHERE f.user.id = :userId)
        """
    )
    fun deleteComponentsByUserId(@Param("userId") userId: Long)

    // 탈퇴 하드삭제: 사용자 프레임 일괄 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Frame f WHERE f.user.id = :userId")
    fun deleteFramesByUserId(@Param("userId") userId: Long)
}