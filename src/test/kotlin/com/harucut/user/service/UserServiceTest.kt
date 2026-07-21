package com.harucut.user.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.storage.service.FileStorageService
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import com.harucut.subscription.usage.SubscriptionUsage
import com.harucut.subscription.usage.SubscriptionUsageService
import com.harucut.user.config.PlanPricingProperties
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.Optional

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val fileStorageService = mockk<FileStorageService>()
    private val subscriptionUsageService = mockk<SubscriptionUsageService>()
    private val planPricingProperties = PlanPricingProperties()
    private val clock = Clock.systemDefaultZone()

    private val service = UserServiceImpl(
        userRepository, userSubscriptionRepository, fileStorageService, subscriptionUsageService, planPricingProperties, clock
    )

    private fun userMock(id: Long = 1L): User = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { email } returns "user@harucut.com"
        every { username } returns "하루컷"
        every { provider } returns Provider.HARUCUT
        every { profileImageUrl } returns "uploads/users/pub/profile/a.png"
    }

    @Nested
    inner class GetUserInfo {

        @Test
        @DisplayName("프로필 presigned URL과 요금제·가격을 채워 반환한다")
        fun success() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userSubscriptionRepository.findByUserId(1L) } returns
                mockk(relaxed = true) { every { effectiveTier(any()) } returns PlanTier.PLUS }
            every { fileStorageService.generatePresignedGetUrl("uploads/users/pub/profile/a.png") } returns "https://profile"

            val res = service.getUserInfo(1L)

            assertThat(res.email).isEqualTo("user@harucut.com")
            assertThat(res.profileUrl).isEqualTo("https://profile")
            assertThat(res.loginPlatform).isEqualTo("HARUCUT")
            assertThat(res.planTier).isEqualTo("PLUS")
            assertThat(res.monthlyPrice).isEqualTo(3900)
        }

        @Test
        @DisplayName("구독이 없으면 기본 요금제(BASIC, 0원)로 반환한다")
        fun noSubscription() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userSubscriptionRepository.findByUserId(1L) } returns null
            every { fileStorageService.generatePresignedGetUrl(any()) } returns "https://profile"

            val res = service.getUserInfo(1L)

            assertThat(res.planTier).isEqualTo("BASIC")
            assertThat(res.monthlyPrice).isEqualTo(0)
        }

        @Test
        @DisplayName("사용자가 없으면 NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { userRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.getUserInfo(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }
    }

    @Nested
    inner class GetSubscriptionUsage {

        @Test
        @DisplayName("구독 사용량 계산을 위임받아 응답 DTO로 매핑한다")
        fun delegatesAndMaps() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { subscriptionUsageService.getUsage(user) } returns SubscriptionUsage(
                planTier = PlanTier.BASIC,
                frameRetentionLimit = 1,
                frameRetentionUsed = 1,
                frameRetentionRemaining = 0,
                frameRetentionUnlimited = false
            )

            val res = service.getSubscriptionUsage(1L)

            assertThat(res.planTier).isEqualTo("BASIC")
            assertThat(res.frameRetentionUsedCount).isEqualTo(1)
            assertThat(res.frameRetentionRemainingCount).isEqualTo(0)
        }

        @Test
        @DisplayName("사용자가 없으면 NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { userRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.getSubscriptionUsage(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }
    }

    @Nested
    inner class ChangeUsername {

        @Test
        @DisplayName("사용자 닉네임을 변경한다")
        fun success() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)

            service.changeUsername(1L, "새이름")

            verify { user.changeUsername("새이름") }
        }

        @Test
        @DisplayName("사용자가 없으면 NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { userRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.changeUsername(1L, "새이름") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }
    }

    @Nested
    inner class ChangeProfileImage {

        @Test
        @DisplayName("입력 URL을 순수 S3 key로 정규화해 반영한다")
        fun normalizesKey() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)

            service.changeProfileImage(
                1L, "https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/users/pub/profile/new.png"
            )

            verify { user.changeProfileImageUrl("uploads/users/pub/profile/new.png") }
        }

        @Test
        @DisplayName("빈 s3Key면 INVALID_INPUT_VALUE 예외를 던진다")
        fun blankKey() {
            val user = userMock(1L)
            every { userRepository.findById(1L) } returns Optional.of(user)

            assertThatThrownBy { service.changeProfileImage(1L, "   ") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
        }
    }
}
