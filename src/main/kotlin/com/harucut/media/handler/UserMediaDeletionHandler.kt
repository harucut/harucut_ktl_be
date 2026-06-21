package com.harucut.media.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.media.repository.UserMediaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserMediaDeletionHandler(
    private val userMediaRepository: UserMediaRepository
) : UserDeletionHandler {

    // 탈퇴 하드삭제 시 사용자 미디어 DB 행 일괄삭제
    @Transactional
    override fun handleUserDeletion(userId: Long) {
        userMediaRepository.deleteByUserId(userId)
    }
}