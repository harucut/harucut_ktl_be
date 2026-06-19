package com.harucut.auth.oauth2

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser

class CustomOAuth2User(
    val user: User,
    private val attributes: Map<String, Any>,
    private val nameAttributeKey: String,
    val provider: Provider,
    private val idToken: OidcIdToken? = null,
    private val userInfo: OidcUserInfo? = null
) : OidcUser {

    val id: Long? = user.id
    val publicId: String = user.publicId
    val status: UserStatus = user.userStatus

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        when (status) {
            UserStatus.DELETED_REQUESTED -> listOf(SimpleGrantedAuthority("ROLE_DELETED_REQUESTED"))
            UserStatus.ACTIVE -> listOf(SimpleGrantedAuthority(user.userRole.name))
            else -> emptyList()
        }

    override fun getClaims(): Map<String, Any> = idToken?.claims ?: emptyMap()

    override fun getUserInfo(): OidcUserInfo? = userInfo

    override fun getIdToken(): OidcIdToken? = idToken

    override fun getName(): String = attributes[nameAttributeKey]?.toString() ?: user.email
}