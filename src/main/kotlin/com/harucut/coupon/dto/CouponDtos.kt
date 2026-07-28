package com.harucut.coupon.dto

import com.harucut.coupon.entity.UserCoupon
import com.harucut.coupon.enums.UserCouponStatus
import com.harucut.subscription.plan.PlanTier
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "쿠폰 사용 요청")
data class RedeemCouponRequest(
    @field:NotBlank(message = "쿠폰 코드는 필수입니다.")
    @Schema(description = "사용 코드", example = "WELCOME-PRO-2026")
    val code: String
)

@Schema(description = "쿠폰 사용 결과")
data class RedeemResultResponse(
    @Schema(description = "즉시 개시 여부 (true=즉시 개시, false=현 주기 후 예약)", example = "true")
    val applied: Boolean,
    @Schema(description = "부여 tier", example = "PRO")
    val grantTier: PlanTier,
    @Schema(description = "무료 grant 개시(예정) 일시", example = "2026-07-28T10:00:00")
    val startsAt: LocalDateTime,
    @Schema(description = "무료 grant 종료(예정) 일시", example = "2026-08-28T10:00:00")
    val endsAt: LocalDateTime
)

@Schema(description = "내 쿠폰 응답")
data class MyCouponResponse(
    @Schema(description = "쿠폰 사용 이력 공개 ID", example = "aB3dE7fG9h")
    val publicId: String,
    @Schema(description = "관리자 라벨", example = "가입 축하 PRO 1개월")
    val couponName: String,
    @Schema(description = "부여 tier", example = "PRO")
    val grantTier: PlanTier,
    @Schema(description = "상태 (RESERVED/REDEEMED)", example = "REDEEMED")
    val status: UserCouponStatus,
    @Schema(description = "코드 사용 시각", example = "2026-07-28T10:00:00")
    val redeemedAt: LocalDateTime
) {
    companion object {
        fun from(userCoupon: UserCoupon) = MyCouponResponse(
            publicId = userCoupon.publicId,
            couponName = userCoupon.coupon.name,
            grantTier = userCoupon.coupon.grantTier,
            status = userCoupon.status,
            redeemedAt = userCoupon.redeemedAt
        )
    }
}
