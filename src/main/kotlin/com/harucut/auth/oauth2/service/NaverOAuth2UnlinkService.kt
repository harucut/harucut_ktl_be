package com.harucut.auth.oauth2.service

import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exit.service.UserExitService
import com.harucut.auth.oauth2.component.NaverDecryptUniqueId
import com.harucut.exception.BusinessException
import com.harucut.user.enums.Provider
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class NaverOAuth2UnlinkService(
    private val decryptUniqueId: NaverDecryptUniqueId,
    private val userRepository: UserRepository,
    private val userExitService: UserExitService
) {

    fun unlink(request: NaverUnlinkRequest) {
        val providerId = decryptUniqueId.handleUnlinkNotification(request)

        val user = userRepository.findByProviderAndProviderId(Provider.NAVER, providerId)
            ?: throw BusinessException(AuthErrorCode.USER_NOT_FOUND)

        userExitService.requestExit(user.id!!)
    }
}