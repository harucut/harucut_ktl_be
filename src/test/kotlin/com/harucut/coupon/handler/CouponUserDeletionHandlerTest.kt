package com.harucut.coupon.handler

import com.harucut.coupon.repository.UserCouponRepository
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CouponUserDeletionHandlerTest {

    private val userCouponRepository = mockk<UserCouponRepository>()
    private val handler = CouponUserDeletionHandler(userCouponRepository)

    @Test
    @DisplayName("탈퇴 시 해당 사용자의 쿠폰 사용 이력을 삭제한다")
    fun deletesUserCoupons() {
        justRun { userCouponRepository.deleteByUserId(7L) }

        handler.handleUserDeletion(7L)

        verify { userCouponRepository.deleteByUserId(7L) }
    }
}
