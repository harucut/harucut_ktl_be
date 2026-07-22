package com.harucut.payment.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.payment.entity.BillingKey
import com.harucut.payment.enums.BillingKeyStatus
import com.harucut.payment.enums.PgProvider
import com.harucut.payment.repository.BillingKeyRepository
import com.harucut.subscription.entity.UserSubscription
import com.harucut.subscription.handler.UserSubscriptionDeletionHandler
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

// FK 순서 회귀 테스트: user_subscription.billing_key_id가 billing_key를 참조하므로,
// 탈퇴 시 user_subscription을 billing_key보다 먼저 삭제해야 한다(ON DELETE RESTRICT 위반 방지).
// MockK 단위테스트로는 실제 FK 제약을 검증할 수 없어 실제 H2 스키마로 확인한다.
@SpringBootTest
class UserDeletionFkOrderTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userSubscriptionRepository: UserSubscriptionRepository

    @Autowired
    lateinit var billingKeyRepository: BillingKeyRepository

    @Autowired
    lateinit var deletionHandlers: List<UserDeletionHandler>

    @Test
    @DisplayName("UserSubscriptionDeletionHandler가 PaymentDataDeletionHandler보다 먼저 실행되도록 순서가 고정되어 있다")
    fun handlersAreOrderedBySubscriptionFirst() {
        val subscriptionHandlerIndex = deletionHandlers.indexOfFirst { it is UserSubscriptionDeletionHandler }
        val paymentHandlerIndex = deletionHandlers.indexOfFirst { it is PaymentDataDeletionHandler }

        assertThat(subscriptionHandlerIndex).isGreaterThanOrEqualTo(0)
        assertThat(paymentHandlerIndex).isGreaterThanOrEqualTo(0)
        assertThat(subscriptionHandlerIndex).isLessThan(paymentHandlerIndex)
    }

    @Test
    @DisplayName("유료 구독(billing_key 보유) 사용자의 탈퇴 삭제가 FK 위반 없이 성공한다")
    fun deletesPaidSubscriberWithoutFkViolation() {
        val user = userRepository.save(
            User(
                provider = Provider.HARUCUT,
                userRole = UserRole.ROLE_USER,
                email = "fkorder@harucut.com",
                username = "tester",
                profileImageUrl = "resources/defaults/userDefaultImage.png",
                userStatus = UserStatus.ACTIVE
            )
        )

        val billingKey = billingKeyRepository.save(BillingKey(user, PgProvider.MOCK, "bk-fkorder"))
        val subscription = userSubscriptionRepository.save(UserSubscription.createDefault(user))
        val now = LocalDateTime.now()
        subscription.activatePaid(PlanTier.PRO, now, now.plusMonths(1), billingKey)
        userSubscriptionRepository.saveAndFlush(subscription)

        assertThatCode {
            deletionHandlers.forEach { it.handleUserDeletion(user.id!!) }
        }.doesNotThrowAnyException()

        assertThat(userSubscriptionRepository.findByUserId(user.id!!)).isNull()
        assertThat(billingKeyRepository.findByUserIdAndStatus(user.id!!, BillingKeyStatus.ACTIVE)).isNull()
    }
}
