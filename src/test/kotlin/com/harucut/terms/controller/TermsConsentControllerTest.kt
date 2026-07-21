package com.harucut.terms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.terms.dto.TermsConsentStatusResponse
import com.harucut.terms.enums.TermsConsentStatus
import com.harucut.terms.exception.TermsErrorCode
import com.harucut.terms.service.TermsService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(TermsConsentController::class)
@Import(SecurityConfig::class)
class TermsConsentControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var termsService: TermsService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @Nested
    @DisplayName("GET /api/auth/terms/consents/me")
    inner class GetMyConsents {

        @Test
        @DisplayName("내 약관 동의 상태를 200으로 반환한다")
        fun success() {
            every { termsService.getMyConsentStatus(1L) } returns listOf(
                TermsConsentStatusResponse(
                    code = "tos",
                    title = "이용약관",
                    required = true,
                    status = TermsConsentStatus.AGREED,
                    agreedVersion = 1,
                    latestVersion = 1
                )
            )

            mockMvc.get("/api/auth/terms/consents/me") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].status") { value("AGREED") }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.get("/api/auth/terms/consents/me").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/auth/terms/consents")
    inner class Consent {

        @Test
        @DisplayName("동의 항목 배열을 받아 처리하고 200을 반환한다")
        fun success() {
            every { termsService.consent(1L, any()) } just Runs

            mockMvc.post("/api/auth/terms/consents") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(listOf(mapOf("code" to "tos", "agreed" to true)))
            }.andExpect {
                status { isOk() }
            }

            verify { termsService.consent(1L, any()) }
        }

        @Test
        @DisplayName("코드가 비어있으면 400을 반환한다")
        fun blankCode() {
            mockMvc.post("/api/auth/terms/consents") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(listOf(mapOf("code" to "", "agreed" to true)))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { termsService.consent(any(), any()) }
        }

        @Test
        @DisplayName("필수 약관 철회 시 서비스가 던진 TERMS-003을 400으로 반환한다")
        fun requiredWithdraw() {
            every { termsService.consent(1L, any()) } throws BusinessException(TermsErrorCode.REQUIRED_TERMS_CANNOT_WITHDRAW)

            mockMvc.post("/api/auth/terms/consents") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(listOf(mapOf("code" to "tos", "agreed" to false)))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("TERMS-003") }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/terms/consents") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(listOf(mapOf("code" to "tos", "agreed" to true)))
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
