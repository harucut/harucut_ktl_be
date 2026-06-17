package com.harucut.util.mail.component

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class VerificationCodeGenerator {

    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val secureRandom = SecureRandom()

    fun generate(): String = buildString(6) {
        repeat(6) { append(chars[secureRandom.nextInt(chars.length)]) }
    }
}