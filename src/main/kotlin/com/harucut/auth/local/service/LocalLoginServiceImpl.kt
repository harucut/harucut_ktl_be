package com.harucut.auth.local.service

import com.harucut.auth.dto.LocalLoginRequest
import com.harucut.auth.dto.LoginResult
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.auth.jwt.dto.AuthTokenCookies
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.RefreshTokenService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.exception.BusinessException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service

@Service
class LocalLoginServiceImpl(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val cookieManager: CookieManager
) : LocalLoginService {

    override fun login(request: LocalLoginRequest): LoginResult {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )
            val principal = authentication.principal as CustomUserPrincipal
            val publicId = principal.publicId

            val accessToken = jwtTokenService.createAccessToken(publicId)
            val refreshToken = jwtTokenService.createRefreshToken(publicId)

            refreshTokenService.saveRefreshToken(publicId, refreshToken)

            return LoginResult(
                cookies = AuthTokenCookies(
                    accessTokenCookie = cookieManager.createTokenCookie(
                        "accessToken",
                        accessToken,
                        jwtTokenService.getAccessTokenValidityMillis()
                    ),
                    refreshTokenCookie = cookieManager.createTokenCookie(
                        "refreshToken",
                        refreshToken,
                        jwtTokenService.getRefreshTokenValidityMillis()
                    )
                ),
                userStatus = principal.status
            )
        } catch (e: BadCredentialsException) {
            throw BusinessException(AuthErrorCode.INVALID_CREDENTIALS)
        } catch (e: AuthenticationException) {
            val cause = e.cause
            if (cause is CustomAuthenticationException) {
                throw BusinessException(cause.errorCode)
            }
            throw BusinessException(AuthErrorCode.AUTHENTICATION_FAILED)
        }
    }
}