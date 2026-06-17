package com.harucut.util.mail.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class EmailVerificationRepository(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val CODE_PREFIX = "email:code:"
        private const val RESET_CODE_PREFIX = "email:reset:code:"
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

    fun verifyAndRemoveCode(email: String, inputCode: String): Boolean {
        val key = CODE_PREFIX + email
        val script = RedisScript.of(
            """
        local stored = redis.call('GET', KEYS[1])
        if stored and stored:lower() == ARGV[1]:lower() then
            redis.call('DEL', KEYS[1])
            return 1
        else
            return 0
        end
        """.trimIndent(),
            Long::class.java
        )
        return redisTemplate.execute(script, listOf(key), inputCode) == 1L
    }

    fun saveResetCode(email: String, code: String) {
        redisTemplate.opsForValue().set(RESET_CODE_PREFIX + email, code, CODE_TTL)
    }

    fun verifyAndRemoveResetCode(email: String, inputCode: String): Boolean {
        val key = RESET_CODE_PREFIX + email
        val script = RedisScript.of(
            """
        local stored = redis.call('GET', KEYS[1])
        if stored and stored:lower() == ARGV[1]:lower() then
            redis.call('DEL', KEYS[1])
            return 1
        else
            return 0
        end
        """.trimIndent(),
            Long::class.java
        )
        return redisTemplate.execute(script, listOf(key), inputCode) == 1L
    }
}