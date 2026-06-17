package com.harucut.auth.jwt.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class JwtCookieManager(
    @Value("\${cookie.domain}") private val domain: String,
    @Value("\${cookie.secure:true}") private val secure: Boolean
) {

    fun createTokenCookie(name: String, value: String, maxAgeMillis: Long): ResponseCookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(Duration.ofMillis(maxAgeMillis))
            .sameSite("Lax")
            .domain(domain)
            .build()

    fun createExpiredCookie(name: String): ResponseCookie =
        ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .domain(domain)
            .build()
}