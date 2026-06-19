package com.harucut.auth.exit.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.oauth2.service.OAuth2UnlinkService
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

class UserExitServiceTest {

    private val userRepository: UserRepository = mockk()
    private val refreshTokenService: RefreshTokenService = mockk()
    private val deletionHandler: UserDeletionHandler = mockk()
    private val unlinkService: OAuth2UnlinkService = mockk()

    private val service = UserExitServiceImpl(
        userRepository,
        refreshTokenService,
        listOf(deletionHandler),
        listOf(unlinkService)
    )

    private fun user(
        status: UserStatus = UserStatus.ACTIVE,
        provider: Provider = Provider.KAKAO,
        deleteRequestedAt: LocalDateTime? = null
    ) = User(
        provider = provider,
        providerId = "provider-id",
        userRole = UserRole.ROLE_USER,
        email = "test@harucut.com",
        username = "tester",
        profileImageUrl = "img.png",
        userStatus = status,
        deleteRequestedAt = deleteRequestedAt
    )

    @Nested
    inner class RequestExit {

        @Test
        @DisplayName("ACTIVE 유저의 탈퇴를 요청하면 DELETED_REQUESTED 로 전이하고 로그아웃시킨다")
        fun success() {
            // given
            val user = user(UserStatus.ACTIVE)
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { refreshTokenService.logout(any()) } just runs

            // when
            service.requestExit(1L)

            // then
            assertThat(user.userStatus).isEqualTo(UserStatus.DELETED_REQUESTED)
            assertThat(user.deleteRequestedAt).isNotNull()
            verify { refreshTokenService.logout(user.publicId) }
        }

        @Test
        @DisplayName("존재하지 않는 유저면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { userRepository.findById(1L) } returns Optional.empty()

            // when & then
            assertThatThrownBy { service.requestExit(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }

    @Nested
    inner class Exit {

        @Test
        @DisplayName("DELETED_REQUESTED 유저를 하드삭제하면 핸들러·unlink·로그아웃 후 DELETED 로 전이한다")
        fun success() {
            // given
            val user = user(UserStatus.DELETED_REQUESTED, deleteRequestedAt = LocalDateTime.now())
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { deletionHandler.handleUserDeletion(1L) } just runs
            every { unlinkService.supports(user.provider) } returns true
            every { unlinkService.unlink(user) } just runs
            every { refreshTokenService.logout(any()) } just runs

            // when
            service.exit(1L)

            // then
            assertThat(user.userStatus).isEqualTo(UserStatus.DELETED)
            verify { deletionHandler.handleUserDeletion(1L) }
            verify { unlinkService.unlink(user) }
            verify { refreshTokenService.logout(user.publicId) }
        }

        @Test
        @DisplayName("provider 를 지원하는 unlink 서비스가 없으면 unlink 를 호출하지 않는다")
        fun noSupportedUnlink() {
            // given
            val user = user(UserStatus.DELETED_REQUESTED, deleteRequestedAt = LocalDateTime.now())
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { deletionHandler.handleUserDeletion(1L) } just runs
            every { unlinkService.supports(user.provider) } returns false
            every { refreshTokenService.logout(any()) } just runs

            // when
            service.exit(1L)

            // then
            assertThat(user.userStatus).isEqualTo(UserStatus.DELETED)
            verify(exactly = 0) { unlinkService.unlink(any()) }
        }

        @Test
        @DisplayName("DELETED_REQUESTED 상태가 아니면 NOT_DELETION_TARGET 예외를 던진다")
        fun notTarget() {
            // given
            val user = user(UserStatus.ACTIVE)
            every { userRepository.findById(1L) } returns Optional.of(user)

            // when & then
            assertThatThrownBy { service.exit(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.NOT_DELETION_TARGET)
        }

        @Test
        @DisplayName("존재하지 않는 유저면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { userRepository.findById(1L) } returns Optional.empty()

            // when & then
            assertThatThrownBy { service.exit(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }

    @Nested
    inner class ReActivate {

        @Test
        @DisplayName("DELETED_REQUESTED 유저를 ACTIVE 로 복구하고 로그아웃시킨다")
        fun success() {
            // given
            val user = user(UserStatus.DELETED_REQUESTED, deleteRequestedAt = LocalDateTime.now())
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { refreshTokenService.logout(any()) } just runs

            // when
            service.reActivate(1L)

            // then
            assertThat(user.userStatus).isEqualTo(UserStatus.ACTIVE)
            assertThat(user.deleteRequestedAt).isNull()
            verify { refreshTokenService.logout(user.publicId) }
        }

        @Test
        @DisplayName("DELETED_REQUESTED 상태가 아니면 NOT_DELETION_TARGET 예외를 던진다")
        fun notTarget() {
            // given
            val user = user(UserStatus.ACTIVE)
            every { userRepository.findById(1L) } returns Optional.of(user)

            // when & then
            assertThatThrownBy { service.reActivate(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.NOT_DELETION_TARGET)
        }

        @Test
        @DisplayName("존재하지 않는 유저면 USER_NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            // given
            every { userRepository.findById(1L) } returns Optional.empty()

            // when & then
            assertThatThrownBy { service.reActivate(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }

}