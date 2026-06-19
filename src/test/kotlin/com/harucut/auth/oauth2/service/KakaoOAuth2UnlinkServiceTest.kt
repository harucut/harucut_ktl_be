package com.harucut.auth.oauth2.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.oauth2.property.KakaoAuthProperties
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.jvm.java

class KakaoOAuth2UnlinkServiceTest {

    private val props = KakaoAuthProperties(
        adminKey = "test-admin-key",
        unlinkUrl = "https://kapi.kakao.com/v1/user/unlink",
        unlinkContentType = "application/x-www-form-urlencoded;charset=utf-8",
        unlinkTargetIdType = "user_id"
    )

    private lateinit var server: MockRestServiceServer
    private lateinit var service: KakaoOAuth2UnlinkService

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        service = KakaoOAuth2UnlinkService(builder.build(), props)
    }

    private fun user(
        provider: Provider = Provider.KAKAO,
        providerId: String? = "kakao-123"
    ) = User(
        provider = provider,
        providerId = providerId,
        userRole = UserRole.ROLE_USER,
        email = "test@harucut.com",
        username = "tester",
        profileImageUrl = "img.png",
        userStatus = UserStatus.DELETED_REQUESTED
    )

    @Nested
    inner class Supports {

        @Test
        @DisplayName("KAKAO provider 면 true 를 반환한다")
        fun kakao() {
            assertThat(service.supports(Provider.KAKAO)).isTrue()
        }

        @Test
        @DisplayName("KAKAO 가 아니면 false 를 반환한다")
        fun notKakao() {
            assertThat(service.supports(Provider.NAVER)).isFalse()
            assertThat(service.supports(Provider.GOOGLE)).isFalse()
        }
    }

    @Nested
    inner class Unlink {

        @Test
        @DisplayName("Admin Key 헤더와 target_id 로 카카오 unlink 를 호출한다")
        fun success() {
            // given
            server.expect(requestTo(props.unlinkUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK ${props.adminKey}"))
                .andRespond(withSuccess())

            // when
            service.unlink(user(providerId = "kakao-123"))

            // then
            server.verify()
        }

        @Test
        @DisplayName("카카오 응답이 에러면 OAUTH2_UNLINK_FAILED 예외를 던진다")
        fun providerError() {
            // given
            server.expect(requestTo(props.unlinkUrl))
                .andRespond(withServerError())

            // when & then
            assertThatThrownBy { service.unlink(user()) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.OAUTH2_UNLINK_FAILED)
        }

        @Test
        @DisplayName("KAKAO 가 아닌 유저면 아무 요청도 보내지 않고 무시한다")
        fun nonKakaoIgnored() {
            // given : server 에 어떤 기대도 등록하지 않음 → 요청이 가면 실패

            // when
            service.unlink(user(provider = Provider.NAVER))

            // then
            server.verify()
        }
    }
}