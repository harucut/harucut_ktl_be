package com.harucut.auth.local.service

import com.harucut.auth.dto.LocalLoginRequest
import com.harucut.auth.dto.LoginResult

interface LocalLoginService {

    fun login(request: LocalLoginRequest): LoginResult
}