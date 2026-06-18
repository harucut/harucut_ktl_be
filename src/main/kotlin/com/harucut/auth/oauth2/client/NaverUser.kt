package com.harucut.auth.oauth2.client

import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.user.OAuth2User

class NaverUser(
    oAuth2User: OAuth2User,
    clientRegistration: ClientRegistration
) : OAuth2ProviderUser(oAuth2User.attributes, clientRegistration) {

    @Suppress("UNCHECKED_CAST")
    private val naverProfile: Map<String, Any>
        get() = attributes["response"] as Map<String, Any>

    override val providerId: String
        get() = naverProfile["id"] as String

    override val email: String
        get() = naverProfile["email"] as String

    override val nickname: String
        get() = naverProfile["nickname"] as? String ?: ""

    override val profileImageUrl: String?
        get() = naverProfile["profile_image"] as? String
}