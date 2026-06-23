package com.harucut.subscription.event

import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import com.harucut.user.event.UserRegisteredEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SubscriptionProvisioningListenerTest {

    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>(relaxed = true)
    private val listener = SubscriptionProvisioningListener(userSubscriptionRepository)

    private fun user(id: Long? = 1L): User = mockk<User>(relaxed = true).also { every { it.id } returns id }

    @Test
    @DisplayName("구독이 없는 신규 회원이면 기본 구독을 생성·저장한다")
    fun createsWhenAbsent() {
        val u = user()
        every { userSubscriptionRepository.findByUserId(1L) } returns null
        every { userSubscriptionRepository.save(any<UserSubscription>()) } returnsArgument 0

        listener.handleUserRegistered(UserRegisteredEvent(u))

        verify { userSubscriptionRepository.save(any<UserSubscription>()) }
    }

    @Test
    @DisplayName("이미 구독이 있으면 중복 생성하지 않는다")
    fun skipsWhenPresent() {
        val u = user()
        every { userSubscriptionRepository.findByUserId(1L) } returns mockk()

        listener.handleUserRegistered(UserRegisteredEvent(u))

        verify(exactly = 0) { userSubscriptionRepository.save(any()) }
    }
}