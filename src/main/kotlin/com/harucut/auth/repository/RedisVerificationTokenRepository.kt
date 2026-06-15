package com.harucut.auth.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisVerificationTokenRepository(
    private val redisTemplate: StringRedisTemplate
) : VerificationTokenRepository {

    companion object {
        private const val KEY_PREFIX = "auth:email:"
    }

    override fun save(email: String, code: String, ttlInSeconds: Long) {
        redisTemplate.opsForValue().set(KEY_PREFIX + email, code, Duration.ofSeconds(ttlInSeconds))
    }

    override fun getCode(email: String): String? =
        redisTemplate.opsForValue().get(KEY_PREFIX + email)

    override fun remove(email: String) {
        redisTemplate.delete(KEY_PREFIX + email)
    }
}
