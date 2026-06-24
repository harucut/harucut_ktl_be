package com.harucut.auth.dto

import com.harucut.user.enums.UserStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "인증 상태 응답 DTO")
data class AuthStatusResponse(
    @Schema(description = "사용자 상태", example = "ACTIVE")
    val userStatus: UserStatus
)
