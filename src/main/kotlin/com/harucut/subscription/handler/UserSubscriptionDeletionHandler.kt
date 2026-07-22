package com.harucut.subscription.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// user_subscription은 billing_key를 FK로 참조하므로, billing_key를 삭제하는
// PaymentDataDeletionHandler보다 먼저 실행되어야 한다(@Order로 순서 고정, FK 위반 방지).
@Component
@Order(1)
class UserSubscriptionDeletionHandler(
    private val userSubscriptionRepository: UserSubscriptionRepository
) : UserDeletionHandler {

    // 탈퇴 하드삭제 시 사용자 구독 DB 행 삭제
    @Transactional
    override fun handleUserDeletion(userId: Long) {
        userSubscriptionRepository.deleteByUserId(userId)
    }
}