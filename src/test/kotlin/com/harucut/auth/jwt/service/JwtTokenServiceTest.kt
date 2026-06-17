package com.harucut.auth.jwt.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.*

class JwtTokenServiceTest {

    private val secret = "test-jwt-secret-key-that-is-at-least-256-bits-long-for-hs256"
    private val accessExpiry = Duration.ofMinutes(30).toMillis()
    private val refreshExpiry = Duration.ofDays(14).toMillis()
    private val service: JwtTokenService = JwtTokenServiceImpl(secret, accessExpiry, refreshExpiry)

    private fun expiredToken(publicId: String): String {
        val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
        val past = Instant.now().minusSeconds(3600)
        return Jwts.builder()
            .setSubject(publicId)
            .setIssuedAt(Date.from(past))
            .setExpiration(Date.from(past.plusSeconds(1)))
            .setIssuer("Harucut")
            .claim("type", "ACCESS")
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    @Nested
    inner class CreateAccessToken {

        @Test
        @DisplayName("ACCESS 타입 토큰을 생성한다")
        fun success() {
            // given
            val publicId = "abc123"

            // when
            val claims = service.parse(service.createAccessToken(publicId))

            // then
            assertThat(claims.publicId).isEqualTo(publicId)
            assertThat(claims.tokenType).isEqualTo("ACCESS")
        }
    }

    @Nested
    inner class CreateRefreshToken {

        @Test
        @DisplayName("REFRESH 타입 토큰을 생성한다")
        fun success() {
            // given
            val publicId = "abc123"

            // when
            val claims = service.parse(service.createRefreshToken(publicId))

            // then
            assertThat(claims.publicId).isEqualTo(publicId)
            assertThat(claims.tokenType).isEqualTo("REFRESH")
        }
    }

    @Nested
    inner class Parse {

        @Test
        @DisplayName("유효한 토큰에서 publicId와 tokenType을 반환한다")
        fun success() {
            // given
            val publicId = "abc123"
            val token = service.createAccessToken(publicId)

            // when
            val claims = service.parse(token)

            // then
            assertThat(claims.publicId).isEqualTo(publicId)
            assertThat(claims.tokenType).isEqualTo("ACCESS")
        }

        @Test
        @DisplayName("만료된 토큰은 EXPIRED_TOKEN 예외를 던진다")
        fun expiredToken() {
            // given
            val token = expiredToken("abc123")

            // when & then
            assertThatThrownBy { service.parse(token) }
                .isInstanceOf(CustomAuthenticationException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EXPIRED_TOKEN)
        }

        @Test
        @DisplayName("위조된 토큰은 INVALID_TOKEN 예외를 던진다")
        fun tamperedToken() {
            // given
            val token = service.createAccessToken("abc123") + "TAMPERED"

            // when & then
            assertThatThrownBy { service.parse(token) }
                .isInstanceOf(CustomAuthenticationException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }

        @Test
        @DisplayName("빈 문자열 토큰은 INVALID_TOKEN 예외를 던진다")
        fun emptyToken() {
            // when & then
            assertThatThrownBy { service.parse("") }
                .isInstanceOf(CustomAuthenticationException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }

        @Test
        @DisplayName("다른 시크릿으로 서명된 토큰은 INVALID_TOKEN 예외를 던진다")
        fun wrongSignature() {
            // given
            val otherService = JwtTokenServiceImpl(
                "other-secret-key-that-is-also-at-least-256-bits-long-hmac",
                accessExpiry,
                refreshExpiry
            )
            val tokenFromOther = otherService.createAccessToken("abc123")

            // when & then
            assertThatThrownBy { service.parse(tokenFromOther) }
                .isInstanceOf(CustomAuthenticationException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }
    }

    @Nested
    inner class GetValidityMillis {

        @Test
        @DisplayName("설정된 accessToken 만료 시간을 반환한다")
        fun accessTokenExpiry() {
            assertThat(service.getAccessTokenValidityMillis()).isEqualTo(accessExpiry)
        }

        @Test
        @DisplayName("설정된 refreshToken 만료 시간을 반환한다")
        fun refreshTokenExpiry() {
            assertThat(service.getRefreshTokenValidityMillis()).isEqualTo(refreshExpiry)
        }
    }
}