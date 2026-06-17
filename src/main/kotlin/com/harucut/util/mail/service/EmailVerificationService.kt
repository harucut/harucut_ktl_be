package com.harucut.util.mail.service

interface EmailVerificationService {

    fun sendVerificationCode(email: String)

    fun verifyCode(email: String, code: String): Boolean

    fun consumeVerified(email: String): Boolean

    fun sendPasswordResetCode(email: String)
}