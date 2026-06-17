package com.harucut.auth.local.service

import com.harucut.auth.dto.LocalRegisterRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import com.harucut.util.mail.service.EmailVerificationService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LocalRegisterServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationService: EmailVerificationService
) : LocalRegisterService {

    @Transactional
    override fun register(request: LocalRegisterRequest) {
        if (isEmailTaken(request.email)) {
            throw BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        if (!emailVerificationService.consumeVerified(request.email)) {
            throw BusinessException(AuthErrorCode.EMAIL_REGISTRATION_FAILED)
        }

        userRepository.save(
            User(
                provider = Provider.HARUCUT,
                userRole = UserRole.ROLE_USER,
                email = request.email,
                username = request.username,
                password = passwordEncoder.encode(request.password),
                profileImageUrl = "resources/defaults/userDefaultImage.png",
                userStatus = UserStatus.ACTIVE
            )
        )
    }

    private fun isEmailTaken(email: String): Boolean =
        userRepository.existsByProviderAndEmail(Provider.HARUCUT, email)
}