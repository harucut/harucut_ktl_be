package com.harucut.auth.local.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.auth.security.service.CustomUserDetailsService
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CustomUserDetailsServiceTest {

    private val userRepository: UserRepository = mockk()
    private val service = CustomUserDetailsService(userRepository)

    private fun user(email: String = "test@harucut.com") = User(
        provider = Provider.HARUCUT,
        userRole = UserRole.ROLE_USER,
        email = email,
        username = "tester",
        profileImageUrl = "default.png",
        userStatus = UserStatus.ACTIVE
    )

    @Nested
    inner class LoadUserByUsername {

        @Test
        @DisplayName("존재하는 이메일로 CustomUserPrincipal을 반환한다")
        fun success() {
            // given
            val email = "test@harucut.com"
            every { userRepository.findByProviderAndEmail(Provider.HARUCUT, email) } returns user(email)

            // when
            val result = service.loadUserByUsername(email)

            // then
            assertThat(result).isInstanceOf(CustomUserPrincipal::class.java)
            assertThat(result!!.username).isEqualTo(email)
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 USER_NOT_FOUND 예외를 던진다")
        fun notFound() {
            // given
            every { userRepository.findByProviderAndEmail(Provider.HARUCUT, any()) } returns null

            // when & then
            assertThatThrownBy { service.loadUserByUsername("no@harucut.com") }
                .isInstanceOf(CustomAuthenticationException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }

    @Nested
    inner class LoadUserByPublicId {

        @Test
        @DisplayName("존재하는 publicId로 CustomUserPrincipal을 반환한다")
        fun success() {
            // given
            every { userRepository.findByPublicId("abc123") } returns user()

            // when
            val result = service.loadUserByPublicId("abc123")

            // then
            assertThat(result).isInstanceOf(CustomUserPrincipal::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 publicId면 USER_NOT_FOUND 예외를 던진다")
        fun notFound() {
            // given
            every { userRepository.findByPublicId(any()) } returns null

            // when & then
            assertThatThrownBy { service.loadUserByPublicId("nonexistent") }
                .isInstanceOf(CustomAuthenticationException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }
}