package com.harucut.auth.security.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.user.enums.Provider
import com.harucut.user.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails? {
        val user = userRepository.findByProviderAndEmail(Provider.HARUCUT, email)
            ?: throw CustomAuthenticationException(AuthErrorCode.USER_NOT_FOUND)

        return CustomUserPrincipal(user)
    }

    fun loadUserByPublicId(publicId: String): CustomUserPrincipal {
        val user = userRepository.findByPublicId(publicId)
            ?: throw CustomAuthenticationException(AuthErrorCode.USER_NOT_FOUND)
        return CustomUserPrincipal(user)
    }
}