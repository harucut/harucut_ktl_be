package com.harucut.auth.jwt.service

import com.harucut.auth.jwt.dto.AuthTokenCookies

interface RefreshTokenService {

    fun saveRefreshToken(publicId: String, refreshToken: String)
    fun reissue(refreshToken: String): AuthTokenCookies
    fun logout(publicId: String)
}