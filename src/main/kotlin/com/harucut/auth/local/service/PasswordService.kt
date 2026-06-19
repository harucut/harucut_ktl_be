package com.harucut.auth.local.service

import com.harucut.auth.dto.PasswordResetTokenResponse

interface PasswordService {

    fun sendResetCode(email: String)
    fun verifyAuthCode(email: String, inputCode: String): PasswordResetTokenResponse
    fun resetPassword(resetToken: String, newPassword: String)
    fun changePassword(userId: Long, encodedPassword: String?, oldPassword: String, newPassword: String)
}