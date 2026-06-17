package com.harucut.auth.jwt.service

import com.harucut.auth.jwt.dto.JwtClaims

interface JwtTokenService {
    fun createAccessToken(publicId: String): String
    fun createRefreshToken(publicId: String): String
    fun parse(token: String): JwtClaims
    fun getAccessTokenValidityMillis(): Long
    fun getRefreshTokenValidityMillis(): Long
}