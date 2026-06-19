package com.harucut.auth.oauth2.component

import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class NaverDecryptUniqueIdTest {

    private val clientSecret = "test-naver-secret"
    private val decryptor = NaverDecryptUniqueId(clientSecret)

    private fun encrypt(plainProviderId: String): String {
        val aesKey = MessageDigest.getInstance("MD5")
            .digest(clientSecret.toByteArray(StandardCharsets.UTF_8))
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))
        }
        val cipherText = cipher.doFinal(plainProviderId.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(iv + cipherText)
    }

    private fun sign(clientId: String, encrypted: String, timestamp: String): String {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(clientSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        }
        val raw = mac.doFinal((clientId + encrypted + timestamp).toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
    }

    @Nested
    @DisplayName("handleUnlinkNotification")
    inner class HandleUnlinkNotification {

        @Test
        @DisplayName("서명이 유효하면 암호화된 고유 ID 를 복호화해 반환한다")
        fun success() {
            // given
            val providerId = "naver-unique-12345"
            val clientId = "naver-client"
            val timestamp = "1718800000000"
            val encrypted = encrypt(providerId)
            val request = NaverUnlinkRequest(
                clientId = clientId,
                encryptUniqueId = encrypted,
                timestamp = timestamp,
                signature = sign(clientId, encrypted, timestamp)
            )

            // when
            val result = decryptor.handleUnlinkNotification(request)

            // then
            assertThat(result).isEqualTo(providerId)
        }

        @Test
        @DisplayName("서명이 일치하지 않으면 OAUTH2_UNLINK_FAILED 예외를 던진다")
        fun invalidSignature() {
            // given
            val encrypted = encrypt("naver-unique-12345")
            val request = NaverUnlinkRequest(
                clientId = "naver-client",
                encryptUniqueId = encrypted,
                timestamp = "1718800000000",
                signature = "tampered-signature"
            )

            // when & then
            assertThatThrownBy { decryptor.handleUnlinkNotification(request) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.OAUTH2_UNLINK_FAILED)
        }
    }
}