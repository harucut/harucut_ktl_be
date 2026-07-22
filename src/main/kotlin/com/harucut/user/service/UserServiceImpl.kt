package com.harucut.user.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.storage.service.FileStorageService
import com.harucut.storage.util.normalizeToS3Key
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.subscription.usage.SubscriptionUsageService
import com.harucut.user.config.PlanPricingProperties
import com.harucut.user.dto.SubscriptionUsageResponse
import com.harucut.user.dto.UserInfoResponse
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val fileStorageService: FileStorageService,
    private val subscriptionUsageService: SubscriptionUsageService,
    private val planPricingProperties: PlanPricingProperties,
    private val clock: Clock
) : UserService {

    // 내 정보 조회 (프로필 presigned URL + 요금제/가격 부착)
    @Transactional(readOnly = true)
    override fun getUserInfo(userId: Long): UserInfoResponse {
        val user = getUserById(userId)
        val planTier = resolvePlanTier(userId)

        val profilePresignedUrl = fileStorageService.generatePresignedGetUrl(user.profileImageUrl)
        return UserInfoResponse(
            id = user.id,
            email = user.email,
            username = user.username,
            profileUrl = profilePresignedUrl,
            loginPlatform = user.provider.name,
            planTier = planTier.name,
            monthlyPrice = planPricingProperties.priceOf(planTier)
        )
    }

    // 구독 사용량 조회 (계산은 subscription 모듈에 위임, API DTO로 매핑)
    @Transactional(readOnly = true)
    override fun getSubscriptionUsage(userId: Long): SubscriptionUsageResponse {
        val user = getUserById(userId)
        return SubscriptionUsageResponse.from(subscriptionUsageService.getUsage(user))
    }

    // 닉네임 변경
    override fun changeUsername(userId: Long, username: String) {
        val user = getUserById(userId)
        user.changeUsername(username)
    }

    // 프로필 이미지 변경 (입력을 순수 S3 key로 정규화 후 반영)
    override fun changeProfileImage(userId: Long, s3Key: String) {
        val user = getUserById(userId)
        user.changeProfileImageUrl(normalizeToS3Key(s3Key))
    }

    // ── helpers ──────────────────────────────

    // userId로 사용자 조회 (없으면 예외)
    private fun getUserById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.") }

    // 구독의 실질 요금제 (없으면 기본 요금제). 정책/사용량과 동일하게 effectiveTier 기준으로 표시한다.
    private fun resolvePlanTier(userId: Long): PlanTier =
        userSubscriptionRepository.findByUserId(userId)?.effectiveTier(LocalDateTime.now(clock)) ?: PlanTier.DEFAULT
}
