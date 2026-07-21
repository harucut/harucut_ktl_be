package com.harucut.subscription.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.service.SubscriptionService
import com.harucut.support.SecurityBeansMockSupport
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(SubscriptionController::class)
@Import(SecurityConfig::class)
class SubscriptionControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var subscriptionService: SubscriptionService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "user@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @Nested
    @DisplayName("GET /api/auth/subscriptions")
    inner class GetMySubscription {

        @Test
        @DisplayName("내 구독 상태를 200으로 반환한다")
        fun success() {
            every { subscriptionService.getMySubscription(1L) } returns
                SubscriptionResponse("PLUS", "ACTIVE", null, null, true)

            mockMvc.get("/api/auth/subscriptions") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.planTier") { value("PLUS") }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.get("/api/auth/subscriptions").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/auth/subscriptions/cancel")
    inner class CancelAutoRenew {

        @Test
        @DisplayName("자동갱신을 해지하면 200을 반환한다")
        fun success() {
            every { subscriptionService.cancelAutoRenew(1L) } just Runs

            mockMvc.post("/api/auth/subscriptions/cancel") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
            }

            verify { subscriptionService.cancelAutoRenew(1L) }
        }

        @Test
        @DisplayName("이미 해지된 구독이면 서비스가 던진 SUBS-005를 409로 반환한다")
        fun alreadyCanceled() {
            every { subscriptionService.cancelAutoRenew(1L) } throws
                BusinessException(SubscriptionErrorCode.ALREADY_CANCELED)

            mockMvc.post("/api/auth/subscriptions/cancel") {
                with(authentication(authToken()))
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.code") { value("SUBS-005") }
            }
        }
    }
}
