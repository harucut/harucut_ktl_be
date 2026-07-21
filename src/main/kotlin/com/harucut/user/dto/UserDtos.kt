package com.harucut.user.dto

import com.harucut.subscription.usage.SubscriptionUsage
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "사용자 정보 조회 응답")
data class UserInfoResponse(
    @Schema(description = "사용자 ID (PK)", example = "1")
    val id: Long?,
    @Schema(description = "사용자 이메일", example = "user@harucut.com")
    val email: String,
    @Schema(description = "사용자 닉네임", example = "하루컷")
    val username: String,
    @Schema(description = "프로필 이미지 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/users/AbCdEf12Gh/profile/123.jpg")
    val profileUrl: String,
    @Schema(description = "로그인 플랫폼", example = "NAVER, KAKAO, HARUCUT")
    val loginPlatform: String,
    @Schema(description = "요금제 단계", example = "BASIC, PLUS, PRO")
    val planTier: String,
    @Schema(description = "월 구독료(원)", example = "3000")
    val monthlyPrice: Int
)

@Schema(description = "사용자 구독 사용량 조회 응답")
data class SubscriptionUsageResponse(
    @Schema(description = "요금제 단계", example = "BASIC")
    val planTier: String,

    @Schema(description = "프레임 동시 보관 한도 (-1이면 무제한)", example = "1")
    val frameRetentionLimit: Int,
    @Schema(description = "현재 보관 중인 프레임 개수", example = "1")
    val frameRetentionUsedCount: Int,
    @Schema(description = "추가 보관 가능 개수 (-1이면 무제한)", example = "0")
    val frameRetentionRemainingCount: Int,
    @Schema(description = "프레임 보관 무제한 여부", example = "false")
    val frameRetentionUnlimited: Boolean
) {
    companion object {
        fun from(usage: SubscriptionUsage) = SubscriptionUsageResponse(
            planTier = usage.planTier.name,
            frameRetentionLimit = usage.frameRetentionLimit,
            frameRetentionUsedCount = usage.frameRetentionUsed,
            frameRetentionRemainingCount = usage.frameRetentionRemaining,
            frameRetentionUnlimited = usage.frameRetentionUnlimited
        )
    }
}

@Schema(description = "프로필 이미지 변경 요청")
data class ChangeProfileImageRequest(
    @field:NotBlank(message = "S3 Key는 필수입니다.")
    @Schema(description = "S3에 업로드된 프로필 이미지 파일의 Key", example = "uploads/users/AbCdEf12Gh/profile/123.jpg")
    val s3Key: String
)
