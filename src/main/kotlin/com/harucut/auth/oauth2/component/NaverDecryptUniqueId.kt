package com.harucut.auth.oauth2.component

import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class NaverDecryptUniqueId(
    @Value("\${spring.security.oauth2.client.registration.naver.client-secret}")
    private val clientSecret: String
) {
    companion object {
        private const val ALGORITHM_AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding"
        private const val ALGORITHM_AES = "AES"
        private const val BLOCK_SIZE = 16
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    fun handleUnlinkNotification(request: NaverUnlinkRequest): String {
        if (!verifySignature(request)) {
            throw BusinessException(AuthErrorCode.OAUTH2_UNLINK_FAILED)
        }
        val aesKey = generateAesKey(clientSecret)
        return decryptUniqueId(request.encryptUniqueId, aesKey)
    }

    private fun verifySignature(request: NaverUnlinkRequest): Boolean {
        val data = request.clientId + request.encryptUniqueId + request.timestamp

        val mac = Mac.getInstance(HMAC_ALGORITHM).apply {
            init(SecretKeySpec(clientSecret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM))
        }
        val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        val calculated = Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac)

        return calculated == request.signature
    }

    private fun generateAesKey(secret: String): ByteArray =
        MessageDigest.getInstance("MD5").digest(secret.toByteArray(StandardCharsets.UTF_8))

    private fun decryptUniqueId(encrypted: String, aesKey: ByteArray): String {
        val encryptedWithIv = Base64.getUrlDecoder().decode(encrypted)
        val iv = encryptedWithIv.copyOfRange(0, BLOCK_SIZE)
        val cipherText = encryptedWithIv.copyOfRange(BLOCK_SIZE, encryptedWithIv.size)

        val cipher = Cipher.getInstance(ALGORITHM_AES_CBC_PKCS5).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, ALGORITHM_AES), IvParameterSpec(iv))
        }
        return String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
    }
}