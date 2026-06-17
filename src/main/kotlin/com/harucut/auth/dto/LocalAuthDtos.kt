package com.harucut.auth.dto

import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.user.enums.UserStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LocalRegisterRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val username: String,
    @field:NotBlank @field:Size(min = 8, max = 20) val password: String
)

data class LocalLoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String
)

data class LoginResponse(val userStatus: UserStatus)

data class LoginResult(
    val cookies: AuthTokenCookies,
    val userStatus: UserStatus
)

data class LocalResetPasswordRequest(
    @field:NotBlank val resetToken: String,
    @field:NotBlank @field:Size(min = 8, max = 20) val newPassword: String
)

data class LocalChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:NotBlank @field:Size(min = 8, max = 20) val newPassword: String
)

data class PasswordResetTokenResponse(val resetToken: String)