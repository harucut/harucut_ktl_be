package com.harucut.subscription.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.exception.SubscriptionErrorCode
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.service.SubscriptionAdminService
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
import org.springframework.test.web.servlet.patch

@WebMvcTest(SubscriptionAdminController::class)
@Import(SecurityConfig::class)
class SubscriptionAdminControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var subscriptionAdminService: SubscriptionAdminService

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "admin@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    @Nested
    @DisplayName("GET /api/admin/subscriptions/{userId}")
    inner class GetSubscription {

        @Test
        @DisplayName("관리자가 구독 상태를 200으로 조회한다")
        fun success() {
            every { subscriptionAdminService.getSubscription(1L) } returns
                SubscriptionResponse("PLUS", "ACTIVE", null, null, true)

            mockMvc.get("/api/admin/subscriptions/1") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.planTier") { value("PLUS") }
            }
        }

        @Test
        @DisplayName("존재하지 않는 구독이면 서비스가 던진 SUBS-004를 404로 반환한다")
        fun notFound() {
            every { subscriptionAdminService.getSubscription(1L) } throws
                BusinessException(SubscriptionErrorCode.NO_ACTIVE_SUBSCRIPTION)

            mockMvc.get("/api/admin/subscriptions/1") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("SUBS-004") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/subscriptions/1") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.get("/api/admin/subscriptions/1").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/subscriptions/{userId}/plan")
    inner class ChangePlan {

        @Test
        @DisplayName("관리자가 요금제를 강제 변경하면 200을 반환한다")
        fun success() {
            every { subscriptionAdminService.changePlan(1L, PlanTier.PRO) } just Runs

            mockMvc.patch("/api/admin/subscriptions/1/plan") {
                with(authentication(authToken("ROLE_ADMIN")))
                param("planTier", "PRO")
            }.andExpect {
                status { isOk() }
            }

            verify { subscriptionAdminService.changePlan(1L, PlanTier.PRO) }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.patch("/api/admin/subscriptions/1/plan") {
                with(authentication(authToken("ROLE_USER")))
                param("planTier", "PRO")
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { subscriptionAdminService.changePlan(any(), any()) }
        }
    }
}
