package com.harucut.auth.oauth2.client

import com.harucut.user.enums.Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.user.OAuth2User

object ProviderUserFactory {

    fun of(
        provider: Provider,
        oAuth2User: OAuth2User,
        clientRegistration: ClientRegistration
    ): ProviderUser = when (provider) {
        Provider.GOOGLE -> GoogleUser(oAuth2User, clientRegistration)
        Provider.KAKAO -> KakaoUser(oAuth2User, clientRegistration)
        Provider.NAVER -> NaverUser(oAuth2User, clientRegistration)
        else -> throw IllegalArgumentException("지원하지 않는 provider: $provider")
    }
}