package com.harucut.auth.oauth2.service

import com.harucut.auth.oauth2.CustomOAuth2User
import com.harucut.auth.oauth2.client.ProviderUser
import com.harucut.auth.oauth2.client.ProviderUserFactory
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLoginService(
    private val userRepository: UserRepository
) {

    companion object {
        private const val DEFAULT_PROFILE_IMAGE = "resources/defaults/userDefaultImage.png"
    }

    @Transactional
    fun processUser(clientRegistration: ClientRegistration, oAuth2User: OAuth2User): CustomOAuth2User {
        val provider = Provider.from(clientRegistration.registrationId)
        val providerUser = ProviderUserFactory.of(provider, oAuth2User, clientRegistration)

        val user = userRepository.findByProviderAndProviderId(provider, providerUser.providerId)
            ?: registerUser(provider, providerUser)

        val nameAttributeKey = clientRegistration.providerDetails
            .userInfoEndpoint
            .userNameAttributeName

        return if (oAuth2User is OidcUser) {
            CustomOAuth2User(
                user, providerUser.attributes, nameAttributeKey, provider,
                oAuth2User.idToken, oAuth2User.userInfo
            )
        } else {
            CustomOAuth2User(user, providerUser.attributes, nameAttributeKey, provider)
        }
    }

    private fun registerUser(
        provider: Provider,
        providerUser: ProviderUser
    ): User {
        val user = User(
            provider = provider,
            providerId = providerUser.providerId,
            userRole = UserRole.ROLE_USER,
            email = providerUser.email,
            username = providerUser.nickname,
            profileImageUrl = DEFAULT_PROFILE_IMAGE,
            userStatus = UserStatus.ACTIVE
        )

        return userRepository.save(user)
    }
}