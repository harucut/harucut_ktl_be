package com.harucut.subscription.handler

import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserSubscriptionDeletionHandlerTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val handler = UserSubscriptionDeletionHandler(userSubscriptionRepository)

    @Test
    @DisplayName("탈퇴 시 해당 사용자의 구독 행을 삭제한다")
    fun deletesSubscription() {
        justRun { userSubscriptionRepository.deleteByUserId(7L) }

        handler.handleUserDeletion(7L)

        verify { userSubscriptionRepository.deleteByUserId(7L) }
    }
}