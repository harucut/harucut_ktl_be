package com.harucut.payment.repository

import com.harucut.payment.entity.Payment
import com.harucut.payment.entity.PaymentOrder
import com.harucut.payment.enums.OrderType
import com.harucut.payment.enums.PaymentMethod
import com.harucut.payment.enums.PaymentStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

// 매출 통계용 집계 쿼리를 실제 H2 스키마로 검증한다(서버측 버킷팅을 위해 기간 필터만 실쿼리로 확인).
@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var paymentOrderRepository: PaymentOrderRepository

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    private fun user(email: String): User = userRepository.save(
        User(
            provider = Provider.HARUCUT,
            userRole = UserRole.ROLE_USER,
            email = email,
            username = "tester",
            profileImageUrl = "resources/defaults/userDefaultImage.png",
            userStatus = UserStatus.ACTIVE
        )
    )

    private fun order(user: User, orderType: OrderType, idempotencyKey: String): PaymentOrder =
        paymentOrderRepository.save(
            PaymentOrder(
                user = user,
                targetTier = PlanTier.PLUS,
                amount = 3900,
                orderType = orderType,
                idempotencyKey = idempotencyKey
            )
        )

    private fun approvedPayment(order: PaymentOrder, amount: Int, approvedAt: LocalDateTime): Payment {
        val payment = paymentRepository.save(Payment(order = order, amount = amount, method = PaymentMethod.BILLING_KEY))
        payment.approve("tx-${payment.id}", approvedAt)
        return paymentRepository.saveAndFlush(payment)
    }

    @Test
    @DisplayName("승인 결제만 기간(approvedAt) 내에서 조회하고 실패 결제·범위 밖 결제는 제외한다")
    fun findApprovedInRange() {
        val user = user("stats@harucut.com")
        val initialOrder = order(user, OrderType.INITIAL, "idem-initial")
        val renewalOrder = order(user, OrderType.RENEWAL, "idem-renewal")

        val approvedInitial = approvedPayment(initialOrder, 3900, LocalDateTime.of(2026, 7, 10, 12, 0))
        val approvedRenewal = approvedPayment(renewalOrder, 3900, LocalDateTime.of(2026, 7, 15, 12, 0))

        val failed = paymentRepository.save(Payment(order = renewalOrder, amount = 3900, method = PaymentMethod.BILLING_KEY))
        failed.fail("DECLINED", "거절")
        paymentRepository.saveAndFlush(failed)

        approvedPayment(initialOrder, 3900, LocalDateTime.of(2026, 8, 1, 0, 0))

        val result = paymentRepository.findApprovedInRange(
            PaymentStatus.APPROVED,
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 8, 1, 0, 0)
        )

        assertThat(result).extracting("id").containsExactlyInAnyOrder(approvedInitial.id, approvedRenewal.id)
        assertThat(result).allMatch { it.status == PaymentStatus.APPROVED }
        assertThat(result.map { it.order.orderType }).containsExactlyInAnyOrder(OrderType.INITIAL, OrderType.RENEWAL)
    }
}
