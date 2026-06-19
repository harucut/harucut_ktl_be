package com.harucut.auth.oauth2.client

import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.user.OAuth2User

class GoogleUser(
    oAuth2User: OAuth2User,
    clientRegistration: ClientRegistration
) : OAuth2ProviderUser(oAuth2User.attributes, clientRegistration) {

    override val providerId: String
        get() = attributes["sub"] as String

    override val email: String
        get() = attributes["email"] as String

    override val nickname: String
        get() = attributes["name"] as? String ?: ""

    override val profileImageUrl: String?
        get() = attributes["picture"] as? String
}