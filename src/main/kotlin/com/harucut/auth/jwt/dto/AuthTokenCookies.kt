package com.harucut.auth.jwt.dto

import org.springframework.http.ResponseCookie

data class AuthTokenCookies(
    val accessTokenCookie: ResponseCookie,
    val refreshTokenCookie: ResponseCookie
)
