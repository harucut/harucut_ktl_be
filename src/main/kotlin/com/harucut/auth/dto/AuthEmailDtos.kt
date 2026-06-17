package com.harucut.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailVerifySendRequest(
    @field:NotBlank @field:Email val email: String
)

data class EmailCodeVerifyRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val code: String
)