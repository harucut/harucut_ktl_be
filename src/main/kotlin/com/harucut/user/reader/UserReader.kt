package com.harucut.user.reader

import com.harucut.auth.oauth2.enums.Provider
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.exception.UserErrorCode
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class UserReader(
    private val userRepository: UserRepository
) {

    fun getById(id: Long): User =
        userRepository.findById(id).orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }

    fun getByPublicId(publicId: String): User =
        userRepository.findByPublicId(publicId)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

    fun getByProviderAndEmail(provider: Provider, email: String): User =
        userRepository.findByProviderAndEmail(provider, email)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

    fun getByProviderAndProviderId(provider: Provider, providerId: String): User =
        userRepository.findByProviderAndProviderId(provider, providerId)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

    fun findByProviderAndEmail(provider: Provider, email: String): User? =
        userRepository.findByProviderAndEmail(provider, email)

    fun existsByProviderAndEmail(provider: Provider, email: String): Boolean =
        userRepository.existsByProviderAndEmail(provider, email)
}