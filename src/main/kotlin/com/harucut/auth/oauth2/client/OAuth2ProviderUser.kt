package com.harucut.auth.oauth2.client

import com.harucut.user.enums.Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration

abstract class OAuth2ProviderUser(
    override val attributes: Map<String, Any>,
    private val clientRegistration: ClientRegistration
) : ProviderUser {

    override val provider: Provider
        get() = Provider.from(clientRegistration.registrationId)
}