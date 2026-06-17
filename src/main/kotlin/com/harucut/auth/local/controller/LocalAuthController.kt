package com.harucut.auth.local.controller

import com.harucut.auth.dto.*
import com.harucut.auth.local.service.LocalLoginService
import com.harucut.auth.local.service.LocalRegisterService
import com.harucut.auth.local.service.PasswordService
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/harucut")
class LocalAuthController(
    private val localRegisterService: LocalRegisterService,
    private val localLoginService: LocalLoginService,
    private val passwordService: PasswordService
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

    @PostMapping("/reset/password/code")
    fun sendResetCode(
        @RequestBody @Valid request: EmailVerifySendRequest
    ): ResponseEntity<Response<Unit>> {
        passwordService.sendResetCode(request.email)
        return Response.ok().toResponseEntity()
    }

    @PostMapping("/reset/password/verification")
    fun verifyAuthCode(
        @RequestBody @Valid request: EmailCodeVerifyRequest
    ): ResponseEntity<Response<PasswordResetTokenResponse>> {
        val result = passwordService.verifyAuthCode(request.email, request.code)
        return Response.ok(result).toResponseEntity()
    }

    @PatchMapping("/reset/password")
    fun resetPassword(
        @RequestBody @Valid request: LocalResetPasswordRequest
    ): ResponseEntity<Response<Unit>> {
        passwordService.resetPassword(request.resetToken, request.newPassword)
        return Response.ok().toResponseEntity()
    }

    @PatchMapping("/change/password")
    fun changePassword(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: LocalChangePasswordRequest
    ): ResponseEntity<Response<Unit>> {
        passwordService.changePassword(principal.id!!, principal.password, request.oldPassword, request.newPassword)
        return Response.ok().toResponseEntity()
    }
}