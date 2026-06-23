package com.harucut.subscription.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserSubscriptionDeletionHandler(
    private val userSubscriptionRepository: UserSubscriptionRepository
) : UserDeletionHandler {

    // 탈퇴 하드삭제 시 사용자 구독 DB 행 삭제
    @Transactional
    override fun handleUserDeletion(userId: Long) {
        userSubscriptionRepository.deleteByUserId(userId)
    }
}