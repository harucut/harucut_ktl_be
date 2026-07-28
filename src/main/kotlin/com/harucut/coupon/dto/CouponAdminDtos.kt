package com.harucut.coupon.dto

import com.harucut.coupon.entity.Coupon
import com.harucut.subscription.plan.PlanTier
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

@Schema(description = "쿠폰 생성 요청")
data class CreateCouponRequest(
    @field:NotBlank(message = "쿠폰 이름은 필수입니다.")
    @Schema(description = "관리자 라벨", example = "가입 축하 PRO 1개월")
    val name: String,

    @field:NotBlank(message = "쿠폰 코드는 필수입니다.")
    @Schema(description = "사용 코드", example = "WELCOME-PRO-2026")
    val code: String,

    @field:NotNull(message = "부여 tier는 필수입니다.")
    @Schema(description = "부여 tier (PLUS/PRO)", example = "PRO")
    val grantTier: PlanTier,

    @field:Positive(message = "사용 상한은 1 이상이어야 합니다.")
    @Schema(description = "전체 사용 상한 (null=무제한)", example = "100", nullable = true)
    val maxRedemptions: Int? = null,

    @Schema(description = "사용 마감 일시 (null=무기한)", example = "2026-12-31T23:59:59", nullable = true)
    val validUntil: LocalDateTime? = null
)

@Schema(description = "관리자 쿠폰 응답")
data class CouponResponse(
    @Schema(description = "쿠폰 공개 ID", example = "aB3dE7fG9h")
    val publicId: String,
    @Schema(description = "관리자 라벨", example = "가입 축하 PRO 1개월")
    val name: String,
    @Schema(description = "사용 코드", example = "WELCOME-PRO-2026")
    val code: String,
    @Schema(description = "부여 tier", example = "PRO")
    val grantTier: PlanTier,
    @Schema(description = "전체 사용 상한 (null=무제한)", example = "100", nullable = true)
    val maxRedemptions: Int?,
    @Schema(description = "사용 마감 일시 (null=무기한)", example = "2026-12-31T23:59:59", nullable = true)
    val validUntil: LocalDateTime?,
    @Schema(description = "활성 여부", example = "true")
    val active: Boolean,
    @Schema(description = "누적 사용 수", example = "3")
    val redeemedCount: Long
) {
    companion object {
        fun from(coupon: Coupon, redeemedCount: Long) = CouponResponse(
            publicId = coupon.publicId,
            name = coupon.name,
            code = coupon.code,
            grantTier = coupon.grantTier,
            maxRedemptions = coupon.maxRedemptions,
            validUntil = coupon.validUntil,
            active = coupon.active,
            redeemedCount = redeemedCount
        )
    }
}
