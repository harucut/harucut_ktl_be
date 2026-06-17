package com.harucut.auth.security

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CustomUserPrincipalTest {

    private fun user(status: UserStatus, role: UserRole = UserRole.ROLE_USER) = User(
        provider = Provider.HARUCUT,
        userRole = role,
        email = "test@harucut.com",
        username = "tester",
        profileImageUrl = "default.png",
        userStatus = status
    )

    @Nested
    inner class GetAuthorities {

        @Test
        @DisplayName("ACTIVE 상태이면 userRole 권한을 반환한다")
        fun active() {
            val principal = CustomUserPrincipal(user(UserStatus.ACTIVE, UserRole.ROLE_USER))

            assertThat(principal.authorities)
                .hasSize(1)
                .extracting("authority")
                .containsExactly("ROLE_USER")
        }

        @Test
        @DisplayName("DELETED_REQUESTED 상태이면 ROLE_DELETED_REQUESTED 권한을 반환한다")
        fun deletedRequested() {
            val principal = CustomUserPrincipal(user(UserStatus.DELETED_REQUESTED))

            assertThat(principal.authorities)
                .hasSize(1)
                .extracting("authority")
                .containsExactly("ROLE_DELETED_REQUESTED")
        }

        @Test
        @DisplayName("BLOCKED 상태이면 빈 권한 목록을 반환한다")
        fun blocked() {
            val principal = CustomUserPrincipal(user(UserStatus.BLOCKED))

            assertThat(principal.authorities).isEmpty()
        }

        @Test
        @DisplayName("DELETED 상태이면 빈 권한 목록을 반환한다")
        fun deleted() {
            val principal = CustomUserPrincipal(user(UserStatus.DELETED))

            assertThat(principal.authorities).isEmpty()
        }
    }

}