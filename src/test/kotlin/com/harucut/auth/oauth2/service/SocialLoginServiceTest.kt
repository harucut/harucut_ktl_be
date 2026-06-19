package com.harucut.auth.oauth2.service

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

class SocialLoginServiceTest {

    private val userRepository: UserRepository = mockk()
    private val service = SocialLoginService(userRepository)

    private fun kakaoRegistration(): ClientRegistration =
        ClientRegistration.withRegistrationId("kakao")
            .clientId("kakao-client-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/kakao")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token")
            .userInfoUri("https://kapi.kakao.com/v1/oidc/userinfo")
            .userNameAttributeName("sub")
            .build()

    private fun naverRegistration(): ClientRegistration =
        ClientRegistration.withRegistrationId("naver")
            .clientId("naver-client-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/naver")
            .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
            .tokenUri("https://nid.naver.com/oauth2.0/token")
            .userInfoUri("https://openapi.naver.com/v1/nid/me")
            .userNameAttributeName("response")
            .build()

    @Nested
    inner class ProcessUser {

        @Test
        @DisplayName("카카오(OIDC) 신규 유저면 회원가입 후 CustomOAuth2User를 반환한다")
        fun kakaoNewUser() {
            // given
            val oidcUser = mockk<OidcUser>()

            every { oidcUser.attributes } returns mapOf(
                "sub" to "kakao-sub-123",
                "email" to "kakao@test.com",
                "nickname" to "kakaoNick",
                "picture" to "http://img/k.png"
            )
            every { oidcUser.idToken } returns null
            every { oidcUser.userInfo } returns null
            every { userRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-sub-123") } returns null

            val saved = slot<User>()
            every { userRepository.save(capture(saved)) } answers { saved.captured }

            // when
            val result = service.processUser(kakaoRegistration(), oidcUser)

            // then
            assertThat(result.provider).isEqualTo(Provider.KAKAO)
            assertThat(saved.captured.provider).isEqualTo(Provider.KAKAO)
            assertThat(saved.captured.providerId).isEqualTo("kakao-sub-123")
            assertThat(saved.captured.email).isEqualTo("kakao@test.com")
            assertThat(saved.captured.username).isEqualTo("kakaoNick")
            assertThat(saved.captured.userStatus).isEqualTo(UserStatus.ACTIVE)
            verify(exactly = 1) { userRepository.save(any()) }
        }

        @Test
        @DisplayName("네이버(OAuth2) 신규 유저면 response 속성으로 회원가입한다")
        fun naverNewUser() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf(
                "response" to mapOf(
                    "id" to "naver-1",
                    "email" to "naver@test.com",
                    "nickname" to "naverNick",
                    "profile_image" to "http://img/n.png"
                )
            )

            every { userRepository.findByProviderAndProviderId(Provider.NAVER, "naver-1") } returns null
            val saved = slot<User>()
            every { userRepository.save(capture(saved)) } answers { saved.captured }

            // when
            val result = service.processUser(naverRegistration(), oAuth2User)

            // then
            assertThat(result.provider).isEqualTo(Provider.NAVER)
            assertThat(saved.captured.providerId).isEqualTo("naver-1")
            assertThat(saved.captured.email).isEqualTo("naver@test.com")
            assertThat(saved.captured.username).isEqualTo("naverNick")
            verify(exactly = 1) { userRepository.save(any()) }
        }

        @Test
        @DisplayName("이미 가입된 유저면 회원가입(save)을 호출하지 않는다")
        fun existingUser() {
            // given
            val oidcUser = mockk<OidcUser>()
            every { oidcUser.attributes } returns mapOf(
                "sub" to "kakao-sub-123",
                "email" to "kakao@test.com",
                "nickname" to "kakaoNick"
            )
            every { oidcUser.idToken } returns null
            every { oidcUser.userInfo } returns null

            val existing = User(
                provider = Provider.KAKAO,
                providerId = "kakao-sub-123",
                userRole = com.harucut.user.enums.UserRole.ROLE_USER,
                email = "kakao@test.com",
                username = "kakaoNick",
                profileImageUrl = "default.png",
                userStatus = UserStatus.ACTIVE
            )
            every { userRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-sub-123") } returns existing

            // when
            val result = service.processUser(kakaoRegistration(), oidcUser)

            // then
            assertThat(result.publicId).isEqualTo(existing.publicId)
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }
}