package com.harucut.coupon.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.coupon.dto.CouponResponse
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.service.CouponAdminService
import com.harucut.exception.BusinessException
import com.harucut.subscription.plan.PlanTier
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
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@WebMvcTest(CouponAdminController::class)
@Import(SecurityConfig::class)
class CouponAdminControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var couponAdminService: CouponAdminService

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "admin@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    @Nested
    @DisplayName("POST /api/admin/coupons")
    inner class CreateCoupon {

        @Test
        @DisplayName("관리자가 쿠폰을 생성하면 200을 반환한다")
        fun success() {
            every {
                couponAdminService.createCoupon("쿠폰", "WELCOME-PRO", PlanTier.PRO, 100, null)
            } just Runs

            mockMvc.post("/api/admin/coupons") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("name" to "쿠폰", "code" to "WELCOME-PRO", "grantTier" to "PRO", "maxRedemptions" to 100)
                )
            }.andExpect {
                status { isOk() }
            }

            verify { couponAdminService.createCoupon("쿠폰", "WELCOME-PRO", PlanTier.PRO, 100, null) }
        }

        @Test
        @DisplayName("이름이 없으면 400을 반환한다")
        fun invalid() {
            mockMvc.post("/api/admin/coupons") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "WELCOME-PRO", "grantTier" to "PRO"))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { couponAdminService.createCoupon(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("BASIC tier면 서비스가 던진 COUPON-003을 400으로 반환한다")
        fun basicTier() {
            every {
                couponAdminService.createCoupon("쿠폰", "WELCOME-BASIC", PlanTier.BASIC, null, null)
            } throws BusinessException(CouponErrorCode.INVALID_GRANT_TIER)

            mockMvc.post("/api/admin/coupons") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("name" to "쿠폰", "code" to "WELCOME-BASIC", "grantTier" to "BASIC")
                )
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("COUPON-003") }
            }
        }

        @Test
        @DisplayName("중복 코드면 서비스가 던진 COUPON-002를 409로 반환한다")
        fun duplicated() {
            every {
                couponAdminService.createCoupon("쿠폰", "WELCOME-PRO", PlanTier.PRO, null, null)
            } throws BusinessException(CouponErrorCode.COUPON_CODE_DUPLICATED)

            mockMvc.post("/api/admin/coupons") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("name" to "쿠폰", "code" to "WELCOME-PRO", "grantTier" to "PRO")
                )
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.code") { value("COUPON-002") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.post("/api/admin/coupons") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("name" to "쿠폰", "code" to "WELCOME-PRO", "grantTier" to "PRO")
                )
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { couponAdminService.createCoupon(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/admin/coupons") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("name" to "쿠폰", "code" to "WELCOME-PRO", "grantTier" to "PRO")
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/admin/coupons")
    inner class GetAllCoupons {

        @Test
        @DisplayName("관리자가 전체 쿠폰 목록을 200으로 조회한다")
        fun success() {
            every { couponAdminService.listCoupons() } returns listOf(
                CouponResponse(
                    publicId = "public-1",
                    name = "쿠폰",
                    code = "WELCOME-PRO",
                    grantTier = PlanTier.PRO,
                    maxRedemptions = null,
                    validUntil = null,
                    active = true,
                    redeemedCount = 3L
                )
            )

            mockMvc.get("/api/admin/coupons") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].code") { value("WELCOME-PRO") }
                jsonPath("$.data[0].redeemedCount") { value(3) }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/coupons") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/coupons/{publicId}/deactivate")
    inner class DeactivateCoupon {

        @Test
        @DisplayName("관리자가 쿠폰을 비활성화하면 200을 반환한다")
        fun success() {
            every { couponAdminService.deactivateCoupon("public-1") } just Runs

            mockMvc.patch("/api/admin/coupons/public-1/deactivate") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
            }

            verify { couponAdminService.deactivateCoupon("public-1") }
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 서비스가 던진 COUPON-001을 404로 반환한다")
        fun notFound() {
            every {
                couponAdminService.deactivateCoupon("public-1")
            } throws BusinessException(CouponErrorCode.COUPON_NOT_FOUND)

            mockMvc.patch("/api/admin/coupons/public-1/deactivate") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("COUPON-001") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.patch("/api/admin/coupons/public-1/deactivate") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { couponAdminService.deactivateCoupon(any()) }
        }
    }
}
