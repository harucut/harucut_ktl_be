package com.harucut.auth.dto

data class NaverUnlinkRequest(
    val clientId: String,
    val encryptUniqueId: String,
    val timestamp: String,
    val signature: String
)