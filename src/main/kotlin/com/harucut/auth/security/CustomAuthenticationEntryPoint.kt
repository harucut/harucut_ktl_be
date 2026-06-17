package com.harucut.auth.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import com.harucut.util.response.Response
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val errorCode = if (authException is CustomAuthenticationException) {
            authException.errorCode
        } else {
            AuthErrorCode.AUTHENTICATION_FAILED
        }

        val body = Response.errorResponse<Unit>(errorCode)
        response.status = errorCode.httpStatus.value()
        response.contentType = "application/json;charset=UTF-8"
        objectMapper.writeValue(response.writer, body)
    }
}