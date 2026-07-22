package com.harucut.admin.stats.controller

import com.harucut.admin.stats.dto.RevenuePoint
import com.harucut.admin.stats.dto.RevenueSummaryResponse
import com.harucut.admin.stats.dto.SubscriptionSnapshotResponse
import com.harucut.admin.stats.enums.Granularity
import com.harucut.admin.stats.service.AdminStatsService
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDate

@WebMvcTest(AdminStatsController::class)
@Import(SecurityConfig::class)
class AdminStatsControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var adminStatsService: AdminStatsService

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "admin@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    @Nested
    @DisplayName("GET /api/admin/stats/revenue")
    inner class GetRevenue {

        @Test
        @DisplayName("관리자가 매출 통계를 200으로 조회한다")
        fun success() {
            every {
                adminStatsService.getRevenueSummary(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 22), Granularity.DAY)
            } returns RevenueSummaryResponse(
                from = LocalDate.of(2026, 7, 1),
                to = LocalDate.of(2026, 7, 22),
                granularity = Granularity.DAY,
                totalAmount = 13800,
                totalCount = 2,
                initialAmount = 3900,
                renewalAmount = 9900,
                series = listOf(RevenuePoint(bucket = "2026-07-10", amount = 13800, count = 2))
            )

            mockMvc.get("/api/admin/stats/revenue?from=2026-07-01&to=2026-07-22&granularity=DAY") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.totalAmount") { value(13800) }
                jsonPath("$.data.series[0].bucket") { value("2026-07-10") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/stats/revenue") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/admin/stats/subscriptions")
    inner class GetSubscriptionSnapshot {

        @Test
        @DisplayName("관리자가 구독 스냅샷을 200으로 조회한다")
        fun success() {
            every { adminStatsService.getSubscriptionSnapshot() } returns SubscriptionSnapshotResponse(
                byTier = mapOf(PlanTier.PLUS to 5L, PlanTier.PRO to 2L),
                byStatus = mapOf(
                    SubscriptionStatus.ACTIVE to 7L,
                    SubscriptionStatus.CANCELED to 1L,
                    SubscriptionStatus.PAST_DUE to 1L,
                    SubscriptionStatus.EXPIRED to 3L
                ),
                autoRenewOn = 6,
                autoRenewOff = 6
            )

            mockMvc.get("/api/admin/stats/subscriptions") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.byTier.PLUS") { value(5) }
                jsonPath("$.data.autoRenewOn") { value(6) }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/stats/subscriptions") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }
}
