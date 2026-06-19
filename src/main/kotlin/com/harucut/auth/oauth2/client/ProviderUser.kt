package com.harucut.auth.oauth2.client

import com.harucut.user.enums.Provider

interface ProviderUser {
    val provider: Provider
    val providerId: String
    val email: String
    val nickname: String
    val profileImageUrl: String?
    val attributes: Map<String, Any>
}