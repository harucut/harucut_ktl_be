package com.harucut.auth.local.controller

import com.harucut.auth.dto.*
import com.harucut.auth.local.service.LocalLoginService
import com.harucut.auth.local.service.LocalRegisterService
import com.harucut.auth.local.service.PasswordService
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth (Local)", description = "이메일/비밀번호 기반 회원가입·로그인 및 비밀번호 관리 API")
@RestController
@RequestMapping("/api/harucut")
class LocalAuthController(
    private val localRegisterService: LocalRegisterService,
    private val localLoginService: LocalLoginService,
    private val passwordService: PasswordService
) {

    @Operation(summary = "이메일 회원가입", description = "이메일/비밀번호로 신규 사용자를 등록합니다. (사전 이메일 인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "회원가입 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 이메일 미인증"),
        ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
    )
    @PostMapping("/register")
    fun register(
        @RequestBody @Valid request: LocalRegisterRequest
    ): ResponseEntity<Response<Unit>> {
        localRegisterService.register(request)

        return Response.ok().toResponseEntity()
    }

    @Operation(
        summary = "이메일 로그인",
        description = "이메일/비밀번호로 로그인하여 Access/Refresh 토큰을 쿠키로 발급받습니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공 (Set-Cookie 로 토큰 발급)"),
        ApiResponse(responseCode = "400", description = "비밀번호 불일치 또는 입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
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

    @Operation(summary = "비밀번호 재설정 코드 발송", description = "가입된 이메일로 비밀번호 재설정 인증 코드를 발송합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "코드 발송 성공"),
        ApiResponse(responseCode = "400", description = "이메일 형식 오류"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자"),
        ApiResponse(responseCode = "500", description = "메일 발송 실패")
    )
    @PostMapping("/reset/password/code")
    fun sendResetCode(
        @RequestBody @Valid request: EmailVerifySendRequest
    ): ResponseEntity<Response<Unit>> {
        passwordService.sendResetCode(request.email)
        return Response.ok().toResponseEntity()
    }

    @Operation(
        summary = "비밀번호 재설정 코드 검증",
        description = "인증 코드 검증 후 비밀번호 재설정에 사용할 리셋 토큰을 발급합니다. (리셋 토큰 10분 유효)"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검증 성공 (리셋 토큰 반환)"),
        ApiResponse(responseCode = "400", description = "코드 불일치 또는 만료")
    )
    @PostMapping("/reset/password/verification")
    fun verifyAuthCode(
        @RequestBody @Valid request: EmailCodeVerifyRequest
    ): ResponseEntity<Response<PasswordResetTokenResponse>> {
        val result = passwordService.verifyAuthCode(request.email, request.code)
        return Response.ok(result).toResponseEntity()
    }

    @Operation(summary = "비밀번호 재설정", description = "리셋 토큰으로 새 비밀번호를 설정합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "재설정 성공"),
        ApiResponse(responseCode = "400", description = "비밀번호 형식 검증 실패"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 리셋 토큰"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @PatchMapping("/reset/password")
    fun resetPassword(
        @RequestBody @Valid request: LocalResetPasswordRequest
    ): ResponseEntity<Response<Unit>> {
        passwordService.resetPassword(request.resetToken, request.newPassword)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "비밀번호 변경", description = "로그인된 사용자가 기존 비밀번호 확인 후 새 비밀번호로 변경합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "변경 성공"),
        ApiResponse(responseCode = "400", description = "기존 비밀번호 불일치 또는 입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/change/password")
    fun changePassword(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: LocalChangePasswordRequest
    ): ResponseEntity<Response<Unit>> {
        passwordService.changePassword(principal.id!!, principal.password, request.oldPassword, request.newPassword)
        return Response.ok().toResponseEntity()
    }
}
