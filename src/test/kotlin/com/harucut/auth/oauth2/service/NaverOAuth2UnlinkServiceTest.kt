package com.harucut.auth.oauth2.service

import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exit.service.UserExitService
import com.harucut.auth.oauth2.component.NaverDecryptUniqueId
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import com.harucut.user.repository.UserRepository

class NaverOAuth2UnlinkServiceTest {

    private val decryptUniqueId: NaverDecryptUniqueId = mockk()
    private val userRepository: UserRepository = mockk()
    private val userExitService: UserExitService = mockk()

    private val service = NaverOAuth2UnlinkService(decryptUniqueId, userRepository, userExitService)

    private val request = NaverUnlinkRequest("cid", "enc", "ts", "sig")

    @Nested
    inner class Unlink {

        @Test
        @DisplayName("복호화한 providerId 로 유저를 찾아 탈퇴 요청한다")
        fun success() {
            // given
            val user = mockk<User>()
            every { user.id } returns 1L
            every { decryptUniqueId.handleUnlinkNotification(request) } returns "naver-123"
            every { userRepository.findByProviderAndProviderId(Provider.NAVER, "naver-123") } returns user
            every { userExitService.requestExit(1L) } just runs

            // when
            service.unlink(request)

            // then
            verify { userExitService.requestExit(1L) }
        }

        @Test
        @DisplayName("해당 providerId 의 유저가 없으면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { decryptUniqueId.handleUnlinkNotification(request) } returns "naver-123"
            every { userRepository.findByProviderAndProviderId(Provider.NAVER, "naver-123") } returns null

            // when & then
            assertThatThrownBy { service.unlink(request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)

            verify(exactly = 0) { userExitService.requestExit(any()) }
        }
    }

}