package com.harucut.auth.local.service

import com.harucut.auth.dto.PasswordResetTokenResponse
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.user.enums.Provider
import com.harucut.user.repository.UserRepository
import com.harucut.util.mail.repository.EmailVerificationRepository
import com.harucut.util.mail.service.EmailVerificationService
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.*

@Service
class PasswordServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val emailVerificationService: EmailVerificationService,
    private val redisTemplate: StringRedisTemplate
) : PasswordService {

    companion object {
        private const val KEY_PREFIX = "reset_token:"
        private val RESET_TOKEN_TTL = Duration.ofMinutes(10)
    }

    override fun sendResetCode(email: String) {
        userRepository.findByProviderAndEmail(Provider.HARUCUT, email)
            ?: throw BusinessException(AuthErrorCode.USER_NOT_FOUND)

        emailVerificationService.sendPasswordResetCode(email)
    }

    override fun verifyAuthCode(
        email: String,
        inputCode: String
    ): PasswordResetTokenResponse {
        if (!emailVerificationRepository.verifyAndRemoveResetCode(email, inputCode)) {
            throw BusinessException(AuthErrorCode.EMAIL_VERIFICATION_FAILED)
        }

        val resetToken = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(KEY_PREFIX + resetToken, email, RESET_TOKEN_TTL)

        return PasswordResetTokenResponse(resetToken)
    }

    @Transactional
    override fun resetPassword(resetToken: String, newPassword: String) {
        val key = KEY_PREFIX + resetToken
        val email = redisTemplate.opsForValue().get(key)
            ?: throw BusinessException(AuthErrorCode.INVALID_TOKEN)

        val user = userRepository.findByProviderAndEmail(Provider.HARUCUT, email)
            ?: throw BusinessException(AuthErrorCode.USER_NOT_FOUND)

        user.changePassword(passwordEncoder.encode(newPassword))
        redisTemplate.delete(key)
    }

    @Transactional
    override fun changePassword(
        userId: Long,
        encodedPassword: String?,
        oldPassword: String,
        newPassword: String
    ) {
        if (!passwordEncoder.matches(oldPassword, encodedPassword)) {
            throw BusinessException(AuthErrorCode.WRONG_PASSWORD)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }

        user.changePassword(passwordEncoder.encode(newPassword))
    }
}