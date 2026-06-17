package com.harucut.auth.jwt.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.jwt.dto.JwtClaims
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.exception.BusinessException
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class RefreshTokenServiceTest {

    private val redisTemplate: StringRedisTemplate = mockk()
    private val valueOps = mockk<ValueOperations<String, String>>()
    private val jwtTokenService: JwtTokenService = mockk(relaxed = true)
    private val cookieManager: CookieManager = mockk(relaxed = true)
    private val service = RefreshTokenServiceImpl(redisTemplate, jwtTokenService, cookieManager)

    init {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Nested
    inner class SaveRefreshToken {

        @Test
        @DisplayName("리프레시 토큰을 Redis에 저장한다")
        fun success() {
            // given
            every { valueOps.set(any<String>(), any<String>(), any<Duration>()) } just Runs

            // when
            service.saveRefreshToken("abc123", "refresh-token")

            // then
            verify { valueOps.set("REFRESH_TOKEN:USER:abc123", "refresh-token", any<Duration>()) }
        }
    }

    @Nested
    inner class Reissue {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 토큰을 재발급한다")
        fun success() {
            // given
            val publicId = "abc123"
            val oldToken = "old-refresh"

            every { jwtTokenService.parse(oldToken) } returns JwtClaims(publicId, "REFRESH")
            every { valueOps.get("REFRESH_TOKEN:USER:$publicId") } returns oldToken
            every { valueOps.set(any<String>(), any<String>(), any<Duration>()) } just Runs

            // when
            val result = service.reissue(oldToken)

            // then
            assertThat(result).isInstanceOf(AuthTokenCookies::class.java)
        }

        @Test
        @DisplayName("REFRESH 타입이 아닌 토큰이면 INVALID_TOKEN 예외를 던진다")
        fun wrongType() {
            // given
            every { jwtTokenService.parse(any()) } returns JwtClaims("abc123", "ACCESS")

            // when & then
            assertThatThrownBy { service.reissue("access-token") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }

        @Test
        @DisplayName("저장된 토큰과 다르면 INVALID_TOKEN 예외를 던진다")
        fun mismatch() {
            // given
            every { jwtTokenService.parse(any()) } returns JwtClaims("abc123", "REFRESH")
            every { valueOps.get(any()) } returns "other-token"

            // when & then
            assertThatThrownBy { service.reissue("my-token") }
                .isInstanceOf(BusinessException::class.java)
        }

        @Test
        @DisplayName("Redis에 토큰이 없으면 INVALID_TOKEN 예외를 던진다")
        fun notFound() {
            // given
            every { jwtTokenService.parse(any()) } returns JwtClaims("abc123", "REFRESH")
            every { valueOps.get(any()) } returns null

            // when & then
            assertThatThrownBy { service.reissue("refresh-token") }
                .isInstanceOf(BusinessException::class.java)
        }

        @Test
        @DisplayName("토큰 파싱 실패 시 INVALID_TOKEN 예외를 던진다")
        fun parseFailed() {
            // given
            every { jwtTokenService.parse(any()) } throws
                    CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN)

            // when & then
            assertThatThrownBy { service.reissue("invalid-token") }
                .isInstanceOf(BusinessException::class.java)
        }
    }

    @Nested
    inner class Logout {

        @Test
        @DisplayName("Redis에서 리프레시 토큰을 삭제한다")
        fun success() {
            // given
            every { redisTemplate.delete(any<String>()) } returns true

            // when
            service.logout("abc123")

            // then
            verify { redisTemplate.delete("REFRESH_TOKEN:USER:abc123") }
        }
    }
}