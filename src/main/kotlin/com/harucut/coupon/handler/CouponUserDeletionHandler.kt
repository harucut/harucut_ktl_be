package com.harucut.coupon.handler

import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.coupon.repository.UserCouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponUserDeletionHandler(
    private val userCouponRepository: UserCouponRepository
) : UserDeletionHandler {

    // 탈퇴 하드삭제 시 사용자 쿠폰 사용 이력 삭제
    @Transactional
    override fun handleUserDeletion(userId: Long) {
        userCouponRepository.deleteByUserId(userId)
    }
}
