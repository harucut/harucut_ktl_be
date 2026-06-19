package com.harucut.auth.exit.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exit.handler.UserDeletionHandler
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.oauth2.service.OAuth2UnlinkService
import com.harucut.exception.BusinessException
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserExitServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenService: RefreshTokenService,
    private val handlers: List<UserDeletionHandler>,
    private val unlinkService: List<OAuth2UnlinkService>
) : UserExitService {

    @Transactional
    override fun requestExit(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }

        user.deleteRequested()
        refreshTokenService.logout(user.publicId)
    }

    @Transactional
    override fun exit(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }

        if (user.userStatus != UserStatus.DELETED_REQUESTED) {
            throw BusinessException(AuthErrorCode.NOT_DELETION_TARGET)
        }

        handlers.forEach { it.handleUserDeletion(userId) }

        unlinkService.firstOrNull { it.supports(user.provider) }?.unlink(user)

        refreshTokenService.logout(user.publicId)
        user.delete()
    }

    @Transactional
    override fun reActivate(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }

        if (user.userStatus != UserStatus.DELETED_REQUESTED) {
            throw BusinessException(AuthErrorCode.NOT_DELETION_TARGET)
        }

        refreshTokenService.logout(user.publicId)
        user.reActivate()
    }
}