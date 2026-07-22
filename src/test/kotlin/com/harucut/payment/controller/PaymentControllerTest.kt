package com.harucut.payment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.payment.exception.PaymentErrorCode
import com.harucut.payment.service.PaymentService
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.plan.PlanTier
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
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
import org.springframework.test.web.servlet.post

@WebMvcTest(PaymentController::class)
@Import(SecurityConfig::class)
class PaymentControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var paymentService: PaymentService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "user@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    private fun requestBody() = mapOf("planTier" to "PLUS", "customerKey" to "customer-1", "authKey" to "auth-1")

    @Nested
    @DisplayName("POST /api/auth/payments/subscribe")
    inner class Subscribe {

        @Test
        @DisplayName("결제에 성공하면 200과 구독 상태를 반환한다")
        fun success() {
            every { paymentService.subscribe(1L, PlanTier.PLUS, "customer-1", "auth-1") } returns
                SubscriptionResponse("PLUS", "ACTIVE", null, null, true)

            mockMvc.post("/api/auth/payments/subscribe") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.planTier") { value("PLUS") }
            }
        }

        @Test
        @DisplayName("서비스가 던진 PAY-002를 402로 반환한다")
        fun paymentFailed() {
            every { paymentService.subscribe(1L, PlanTier.PLUS, "customer-1", "auth-1") } throws
                BusinessException(PaymentErrorCode.PAYMENT_FAILED)

            mockMvc.post("/api/auth/payments/subscribe") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isEqualTo(402) }
                jsonPath("$.code") { value("PAY-002") }
            }
        }

        @Test
        @DisplayName("planTier가 없으면 400을 반환한다")
        fun missingPlanTier() {
            mockMvc.post("/api/auth/payments/subscribe") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("customerKey" to "customer-1", "authKey" to "auth-1"))
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/payments/subscribe") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
