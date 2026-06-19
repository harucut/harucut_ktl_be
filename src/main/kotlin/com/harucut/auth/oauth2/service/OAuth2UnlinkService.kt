package com.harucut.auth.oauth2.service

import com.harucut.user.entity.User
import com.harucut.user.enums.Provider

interface OAuth2UnlinkService {
    fun supports(provider: Provider): Boolean
    fun unlink(user: User)
}