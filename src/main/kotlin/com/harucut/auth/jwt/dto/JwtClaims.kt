package com.harucut.auth.jwt.dto

data class JwtClaims(
    val publicId: String,
    val tokenType: String
)