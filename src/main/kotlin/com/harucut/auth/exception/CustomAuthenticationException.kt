package com.harucut.auth.exception

import org.springframework.security.core.AuthenticationException

class CustomAuthenticationException(
    val errorCode: AuthErrorCode
) : AuthenticationException(errorCode.message) {
}