package com.harucut.auth.local.service

import com.harucut.auth.dto.LocalRegisterRequest

interface LocalRegisterService {

    fun register(request: LocalRegisterRequest)
}