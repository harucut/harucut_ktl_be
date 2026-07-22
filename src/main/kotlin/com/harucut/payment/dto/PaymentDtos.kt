package com.harucut.payment.dto

import com.harucut.subscription.plan.PlanTier
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "구독 결제 요청")
data class SubscribeRequest(
    @field:NotNull(message = "요금제 단계는 필수입니다.")
    @Schema(description = "구독할 요금제 단계 (BASIC 불가)", example = "PLUS")
    val planTier: PlanTier,

    @field:NotBlank(message = "고객 식별자는 필수입니다.")
    @Schema(description = "PG 고객 식별자", example = "customer-abc123")
    val customerKey: String,

    @field:NotBlank(message = "인증 키는 필수입니다.")
    @Schema(description = "카드 등록 인증 후 발급된 authKey", example = "auth-abc123")
    val authKey: String
)
