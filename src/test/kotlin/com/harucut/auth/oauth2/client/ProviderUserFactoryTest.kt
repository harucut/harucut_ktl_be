package com.harucut.auth.oauth2.client

import com.harucut.user.enums.Provider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.user.OAuth2User

class ProviderUserFactoryTest {

    private fun registration(id: String): ClientRegistration =
        ClientRegistration.withRegistrationId(id)
            .clientId("id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/$id")
            .authorizationUri("https://example.com/authorize")
            .tokenUri("https://example.com/token")
            .userInfoUri("https://example.com/userinfo")
            .userNameAttributeName("sub")
            .build()

    @Nested
    inner class Of {

        @Test
        @DisplayName("KAKAO면 KakaoUser를 생성한다")
        fun kakao() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf("sub" to "1")

            // when
            val result = ProviderUserFactory.of(Provider.KAKAO, oAuth2User, registration("kakao"))

            // then
            assertThat(result).isInstanceOf(KakaoUser::class.java)
        }

        @Test
        @DisplayName("NAVER면 NaverUser를 생성한다")
        fun naver() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf("response" to mapOf("id" to "1"))

            // when
            val result = ProviderUserFactory.of(Provider.NAVER, oAuth2User, registration("naver"))

            // then
            assertThat(result).isInstanceOf(NaverUser::class.java)
        }

        @Test
        @DisplayName("GOOGLE이면 GoogleUser를 생성한다")
        fun google() {
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf("sub" to "1")

            val result = ProviderUserFactory.of(Provider.GOOGLE, oAuth2User, registration("google"))

            assertThat(result).isInstanceOf(GoogleUser::class.java)
        }

        @Test
        @DisplayName("지원하지 않는 provider면 IllegalArgumentException을 던진다")
        fun unsupported() {
            // given
            val oAuth2User = mockk<OAuth2User>()

            // when & then
            assertThatThrownBy { ProviderUserFactory.of(Provider.APPLE, oAuth2User, registration("apple")) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}