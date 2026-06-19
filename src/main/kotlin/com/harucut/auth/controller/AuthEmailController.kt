package com.harucut.auth.controller

import com.harucut.auth.dto.EmailCodeVerifyRequest
import com.harucut.auth.dto.EmailVerifySendRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.exception.BusinessException
import com.harucut.util.mail.service.EmailVerificationService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth (Email)", description = "회원가입 시 이메일 인증 코드 발송 및 검증 API")
@RestController
@RequestMapping("/api/email-auth")
class AuthEmailController(
    private val emailVerificationService: EmailVerificationService
) {

    @Operation(summary = "인증 코드 발송", description = "입력한 이메일로 6자리 인증 코드를 발송합니다. (코드 5분 유효)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "발송 성공"),
        ApiResponse(responseCode = "400", description = "이메일 형식 오류"),
        ApiResponse(responseCode = "500", description = "메일 발송 실패")
    )
    @PostMapping("/code")
    fun sendVerificationCode(@RequestBody @Valid request: EmailVerifySendRequest): ResponseEntity<Response<Unit>> {
        emailVerificationService.sendVerificationCode(request.email)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "인증 코드 검증", description = "이메일로 발송된 인증 코드를 검증합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검증 성공"),
        ApiResponse(responseCode = "400", description = "코드 불일치 또는 만료")
    )
    @PostMapping("/verification")
    fun verifyCode(@RequestBody @Valid request: EmailCodeVerifyRequest): ResponseEntity<Response<Unit>> {
        if (!emailVerificationService.verifyCode(request.email, request.code)) {
            throw BusinessException(AuthErrorCode.EMAIL_VERIFICATION_FAILED)
        }
        return Response.ok().toResponseEntity()
    }
}
