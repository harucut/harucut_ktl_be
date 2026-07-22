package com.harucut.payment.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.payment.repository.BillingKeyRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// 탈퇴 하드삭제 시 빌링키(카드 토큰)만 삭제한다. payment_order/payment는 개인정보를 담지 않는
// 결제 이력(회계/분쟁 대응 목적으로 보존 필요)이라 삭제하지 않는다 — 별도 익명화도 불필요.
// (탈퇴는 users 행 자체를 하드삭제하지 않고 UserExitServiceImpl.exit()에서 User.delete()로
// 익명화만 하므로 payment_order/payment의 user FK는 계속 유효하다.)
// billing_key는 user_subscription의 FK 참조 대상이므로, user_subscription을 먼저 삭제하는
// UserSubscriptionDeletionHandler(@Order(1))보다 뒤에 실행되어야 한다(FK 위반 방지).
@Component
@Order(2)
class PaymentDataDeletionHandler(
    private val billingKeyRepository: BillingKeyRepository
) : UserDeletionHandler {

    @Transactional
    override fun handleUserDeletion(userId: Long) {
        billingKeyRepository.deleteByUserId(userId)
    }
}
