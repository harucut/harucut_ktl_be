package com.harucut.frame.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.frame.repository.FrameRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// 탈퇴 하드삭제 시 사용자 프레임/컴포넌트 DB 행 일괄삭제
@Component
class FrameUserDeletionHandler(
    private val frameRepository: FrameRepository
) : UserDeletionHandler {

    // 컴포넌트 → 프레임 순서로 삭제 (FK 제약 준수)
    @Transactional
    override fun handleUserDeletion(userId: Long) {
        frameRepository.deleteComponentsByUserId(userId)
        frameRepository.deleteFramesByUserId(userId)
    }
}