package com.harucut.auth.local.controller

import com.harucut.auth.dto.LocalLoginRequest
import com.harucut.auth.dto.LocalRegisterRequest
import com.harucut.auth.dto.LoginResponse
import com.harucut.auth.local.service.LocalLoginService
import com.harucut.auth.local.service.LocalRegisterService
import com.harucut.util.response.Response
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/harucut")
class LocalAuthController(
    private val localRegisterService: LocalRegisterService,
    private val localLoginService: LocalLoginService
) {

    @PostMapping("/register")
    fun register(
        @RequestBody @Valid request: LocalRegisterRequest
    ): ResponseEntity<Response<Unit>> {
        localRegisterService.register(request)

        return Response.ok().toResponseEntity()
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: LocalLoginRequest
    ): ResponseEntity<Response<LoginResponse>> {
        val result = localLoginService.login(request)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, result.cookies.accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, result.cookies.refreshTokenCookie.toString())
            .body(Response.ok(LoginResponse(result.userStatus)))
    }
}