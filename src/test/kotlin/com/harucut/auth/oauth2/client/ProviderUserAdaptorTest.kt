package com.harucut.auth.oauth2.client

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.user.OAuth2User

class ProviderUserAdaptorTest {

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
    inner class Kakao {

        @Test
        @DisplayName("OIDC 클레임에서 providerId/email/nickname/picture를 추출한다")
        fun extract() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf(
                "sub" to "kakao-1",
                "email" to "k@test.com",
                "nickname" to "kakaoNick",
                "picture" to "http://img/k.png"
            )

            // when
            val user = KakaoUser(oAuth2User, registration("kakao"))

            // then
            assertThat(user.providerId).isEqualTo("kakao-1")
            assertThat(user.email).isEqualTo("k@test.com")
            assertThat(user.nickname).isEqualTo("kakaoNick")
            assertThat(user.profileImageUrl).isEqualTo("http://img/k.png")
        }

        @Test
        @DisplayName("nickname이 없으면 빈 문자열, picture가 없으면 null을 반환한다")
        fun missingOptional() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf("sub" to "kakao-1", "email" to "k@test.com")

            // when
            val user = KakaoUser(oAuth2User, registration("kakao"))

            // then
            assertThat(user.nickname).isEmpty()
            assertThat(user.profileImageUrl).isNull()
        }
    }

    @Nested
    inner class Naver {

        @Test
        @DisplayName("response 객체에서 속성을 추출한다")
        fun extract() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf(
                "response" to mapOf(
                    "id" to "naver-1",
                    "email" to "n@test.com",
                    "nickname" to "naverNick",
                    "profile_image" to "http://img/n.png"
                )
            )

            // when
            val user = NaverUser(oAuth2User, registration("naver"))

            // then
            assertThat(user.providerId).isEqualTo("naver-1")
            assertThat(user.email).isEqualTo("n@test.com")
            assertThat(user.nickname).isEqualTo("naverNick")
            assertThat(user.profileImageUrl).isEqualTo("http://img/n.png")
        }

        @Test
        @DisplayName("nickname이 없으면 빈 문자열을 반환한다")
        fun missingNickname() {
            // given
            val oAuth2User = mockk<OAuth2User>()
            every { oAuth2User.attributes } returns mapOf(
                "response" to mapOf("id" to "naver-1", "email" to "n@test.com")
            )

            // when
            val user = NaverUser(oAuth2User, registration("naver"))

            // then
            assertThat(user.nickname).isEmpty()
        }
    }
}