package com.harucut.auth.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.exception.CustomAuthenticationException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.InsufficientAuthenticationException

class CustomAuthenticationEntryPointTest {

    private val objectMapper = ObjectMapper()
    private val entryPoint = CustomAuthenticationEntryPoint(objectMapper)

    @Nested
    inner class Commence {

        @Test
        @DisplayName("CustomAuthenticationException이면 해당 에러코드로 응답한다")
        fun customAuthException() {
            // given
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val exception = CustomAuthenticationException(AuthErrorCode.EXPIRED_TOKEN)

            // when
            entryPoint.commence(request, response, exception)

            // then
            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains(AuthErrorCode.EXPIRED_TOKEN.code)
        }

        @Test
        @DisplayName("일반 AuthenticationException이면 AUTHENTICATION_FAILED로 응답한다")
        fun generalAuthException() {
            // given
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val exception = InsufficientAuthenticationException("insufficient")

            // when
            entryPoint.commence(request, response, exception)

            // then
            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains(AuthErrorCode.AUTHENTICATION_FAILED.code)
        }
    }

}