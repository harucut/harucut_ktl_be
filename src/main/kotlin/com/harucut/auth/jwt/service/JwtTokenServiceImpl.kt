package com.harucut.auth.jwt.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.dto.JwtClaims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.security.oauth2.jwt.JwtException
import java.nio.charset.StandardCharsets
import java.security.Key
import java.time.Instant
import java.util.*

class JwtTokenServiceImpl(
    secretKey: String,
    private val accessTokenValidityMillis: Long,
    private val refreshTokenValidityMillis: Long
) : JwtTokenService {

    private val key: Key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))

    override fun createAccessToken(publicId: String): String =
        createToken(publicId, accessTokenValidityMillis, "ACCESS")

    override fun createRefreshToken(publicId: String): String =
        createToken(publicId, refreshTokenValidityMillis, "REFRESH")

    private fun createToken(publicId: String, validityMillis: Long, type: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .setSubject(publicId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(validityMillis)))
            .setIssuer("Harucut")
            .claim("type", type)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun parse(token: String): JwtClaims {
        try {
            val body = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            return JwtClaims(
                publicId = body.subject,
                tokenType = body.get("type", String::class.java)
            )
        } catch (e: ExpiredJwtException) {
            throw CustomAuthenticationException(AuthErrorCode.EXPIRED_TOKEN)
        } catch (e: SignatureException) {
            throw CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN)
        } catch (e: JwtException) {
            throw CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN)
        } catch (e: IllegalArgumentException) {
            throw CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN)
        }
    }

    override fun getAccessTokenValidityMillis(): Long = accessTokenValidityMillis

    override fun getRefreshTokenValidityMillis(): Long = refreshTokenValidityMillis
}