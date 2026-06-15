package com.harucut.auth.repository

interface VerificationTokenRepository {
    fun save(email: String, code: String, ttlInSeconds: Long)
    fun getCode(email: String): String?
    fun remove(email: String)
}
