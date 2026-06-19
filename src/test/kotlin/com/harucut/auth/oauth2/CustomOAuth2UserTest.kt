package com.harucut.auth.oauth2

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CustomOAuth2UserTest {

    private fun user(status: UserStatus, role: UserRole = UserRole.ROLE_USER): User =
        User(
            provider = Provider.KAKAO,
            providerId = "kakao-1",
            userRole = role,
            email = "test@harucut.com",
            username = "tester",
            profileImageUrl = "default.png",
            userStatus = status
        )

    private fun principal(user: User, attributes: Map<String, Any> = mapOf("sub" to "kakao-1")) =
        CustomOAuth2User(user, attributes, "sub", Provider.KAKAO)

    @Nested
    inner class Authorities {

        @Test
        @DisplayName("ACTIVE면 userRole 권한을 가진다")
        fun active() {
            // when
            val authorities = principal(user(UserStatus.ACTIVE)).authorities

            // then
            assertThat(authorities).extracting("authority").containsExactly("ROLE_USER")
        }

        @Test
        @DisplayName("DELETED_REQUESTED면 ROLE_DELETED_REQUESTED 권한을 가진다")
        fun deletedRequested() {
            // when
            val authorities = principal(user(UserStatus.DELETED_REQUESTED)).authorities

            // then
            assertThat(authorities).extracting("authority").containsExactly("ROLE_DELETED_REQUESTED")
        }

        @Test
        @DisplayName("BLOCKED/DELETED면 권한이 비어있다")
        fun noAuthority() {
            // when & then
            assertThat(principal(user(UserStatus.BLOCKED)).authorities).isEmpty()
            assertThat(principal(user(UserStatus.DELETED)).authorities).isEmpty()
        }
    }

    @Nested
    inner class Name {

        @Test
        @DisplayName("nameAttributeKey 값이 있으면 그 값을 반환한다")
        fun fromAttribute() {
            // given
            val principal = principal(user(UserStatus.ACTIVE), mapOf("sub" to "kakao-1"))

            // when & then
            assertThat(principal.name).isEqualTo("kakao-1")
        }

        @Test
        @DisplayName("nameAttributeKey 값이 없으면 email로 폴백한다")
        fun fallbackToEmail() {
            // given
            val principal = principal(user(UserStatus.ACTIVE), emptyMap())

            // when & then
            assertThat(principal.name).isEqualTo("test@harucut.com")
        }
    }
}