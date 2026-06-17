package com.harucut.auth.security

import com.harucut.user.entity.User
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserPrincipal(user: User) : UserDetails {

    val id: Long? = user.id
    val publicId: String = user.publicId
    val email: String = user.email
    private val encodedPassword: String? = user.password
    val userRole: UserRole = user.userRole
    val status: UserStatus = user.userStatus

    override fun getAuthorities(): Collection<GrantedAuthority> =
        when (status) {
            UserStatus.DELETED_REQUESTED -> listOf(SimpleGrantedAuthority("ROLE_DELETED_REQUESTED"))
            UserStatus.ACTIVE -> listOf(SimpleGrantedAuthority(userRole.name))
            else -> emptyList()
        }

    override fun getPassword(): String? = encodedPassword

    override fun getUsername(): String = email
}