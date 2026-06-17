package com.harucut.auth.controller

import com.harucut.auth.dto.EmailCodeVerifyRequest
import com.harucut.auth.dto.EmailVerifySendRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.util.mail.service.EmailVerificationService
import com.harucut.util.response.Response
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/email-auth")
class AuthEmailController(
    private val emailVerificationService: EmailVerificationService
) {

    @PostMapping("/code")
    fun sendVerificationCode(@RequestBody @Valid request: EmailVerifySendRequest): ResponseEntity<Response<Unit>> {
        emailVerificationService.sendVerificationCode(request.email)
        return Response.ok().toResponseEntity()
    }

    @PostMapping("/verification")
    fun verifyCode(@RequestBody @Valid request: EmailCodeVerifyRequest): ResponseEntity<Response<Unit>> {
        if (!emailVerificationService.verifyCode(request.email, request.code)) {
            throw BusinessException(AuthErrorCode.EMAIL_AUTH_FAILED)
        }
        return Response.ok().toResponseEntity()
    }
}