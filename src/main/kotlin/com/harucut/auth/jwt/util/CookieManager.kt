package com.harucut.auth.jwt.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CookieManager(
    @Value("\${cookie.domain}") private val domain: String,
    @Value("\${cookie.secure:true}") private val secure: Boolean,
    @Value("\${cookie.same-site:Lax}") private val sameSite: String
) {

    fun createTokenCookie(name: String, value: String, maxAgeMillis: Long): ResponseCookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(Duration.ofMillis(maxAgeMillis))
            .sameSite(sameSite)
            .domain(domain)
            .build()

    fun createExpiredCookie(name: String): ResponseCookie =
        ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(0)
            .sameSite(sameSite)
            .domain(domain)
            .build()
}