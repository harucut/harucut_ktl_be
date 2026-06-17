package com.harucut.util.mail.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class EmailVerificationRepository(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val CODE_PREFIX = "email:code:"
        private const val VERIFIED_PREFIX = "email:verified:"
        private val CODE_TTL = Duration.ofMinutes(5)
        private val VERIFIED_TTL = Duration.ofMinutes(10)
    }

    fun saveCode(email: String, code: String) {
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_TTL)
    }

    fun getCode(email: String): String? =
        redisTemplate.opsForValue().get(CODE_PREFIX + email)

    fun removeCode(email: String) {
        redisTemplate.delete(CODE_PREFIX + email)
    }

    fun markVerified(email: String) {
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "VERIFIED", VERIFIED_TTL)
    }

    fun consumeVerified(email: String): Boolean {
        val key = VERIFIED_PREFIX + email
        return redisTemplate.delete(key) ?: false
    }
}