package com.harucut.auth.jwt.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.exception.BusinessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenServiceImpl(
    private val redisTemplate: StringRedisTemplate,
    private val jwtTokenService: JwtTokenService,
    private val cookieManager: CookieManager
) : RefreshTokenService {

    companion object {
        private const val KEY_PREFIX = "REFRESH_TOKEN:USER:"
    }

    private fun buildKey(publicId: String) = "$KEY_PREFIX$publicId"

    override fun saveRefreshToken(publicId: String, refreshToken: String) {
        val ttl = Duration.ofMillis(jwtTokenService.getRefreshTokenValidityMillis())
        redisTemplate.opsForValue().set(buildKey(publicId), refreshToken, ttl)
    }

    override fun reissue(refreshToken: String): AuthTokenCookies {
        val claims = try {
            jwtTokenService.parse(refreshToken)
        } catch (e: CustomAuthenticationException) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }

        if (claims.tokenType != "REFRESH") {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }

        val stored = redisTemplate.opsForValue().get(buildKey(claims.publicId))
        if (stored == null || stored != refreshToken) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }

        val newAccessToken = jwtTokenService.createAccessToken(claims.publicId)
        val newRefreshToken = jwtTokenService.createRefreshToken(claims.publicId)

        redisTemplate.opsForValue().set(
            buildKey(claims.publicId),
            newRefreshToken,
            Duration.ofMillis(jwtTokenService.getRefreshTokenValidityMillis())
        )

        return AuthTokenCookies(
            accessTokenCookie = cookieManager.createTokenCookie(
                "accessToken",
                newAccessToken,
                jwtTokenService.getAccessTokenValidityMillis()
            ),
            refreshTokenCookie = cookieManager.createTokenCookie(
                "refreshToken",
                newRefreshToken,
                jwtTokenService.getRefreshTokenValidityMillis()
            )
        )
    }

    override fun logout(publicId: String) {
        redisTemplate.delete(buildKey(publicId))
    }
}