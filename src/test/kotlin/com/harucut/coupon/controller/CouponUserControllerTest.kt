package com.harucut.coupon.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.coupon.dto.MyCouponResponse
import com.harucut.coupon.dto.RedeemResultResponse
import com.harucut.coupon.enums.UserCouponStatus
import com.harucut.coupon.exception.CouponErrorCode
import com.harucut.coupon.service.CouponService
import com.harucut.exception.BusinessException
import com.harucut.subscription.plan.PlanTier
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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
import java.time.LocalDateTime

@WebMvcTest(CouponUserController::class)
@Import(SecurityConfig::class)
class CouponUserControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var couponService: CouponService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @Nested
    @DisplayName("POST /api/auth/coupons/redeem")
    inner class Redeem {

        @Test
        @DisplayName("쿠폰을 사용하면 200을 반환한다")
        fun success() {
            val now = LocalDateTime.now()
            every { couponService.redeem(1L, "WELCOME-PRO") } returns RedeemResultResponse(
                applied = true, grantTier = PlanTier.PRO, startsAt = now, endsAt = now.plusMonths(1)
            )

            mockMvc.post("/api/auth/coupons/redeem") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "WELCOME-PRO"))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.applied") { value(true) }
                jsonPath("$.data.grantTier") { value("PRO") }
            }

            verify { couponService.redeem(1L, "WELCOME-PRO") }
        }

        @Test
        @DisplayName("코드가 비어있으면 400을 반환한다")
        fun blankCode() {
            mockMvc.post("/api/auth/coupons/redeem") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to ""))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { couponService.redeem(any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/coupons/redeem") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "WELCOME-PRO"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        @DisplayName("존재하지 않는 코드면 서비스가 던진 COUPON-001을 404로 반환한다")
        fun notFound() {
            every {
                couponService.redeem(1L, "NO-CODE")
            } throws BusinessException(CouponErrorCode.COUPON_NOT_FOUND)

            mockMvc.post("/api/auth/coupons/redeem") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "NO-CODE"))
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("COUPON-001") }
            }
        }

        @Test
        @DisplayName("이미 사용한 쿠폰이면 서비스가 던진 COUPON-006을 409로 반환한다")
        fun alreadyRedeemed() {
            every {
                couponService.redeem(1L, "WELCOME-PRO")
            } throws BusinessException(CouponErrorCode.COUPON_ALREADY_REDEEMED)

            mockMvc.post("/api/auth/coupons/redeem") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "WELCOME-PRO"))
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.code") { value("COUPON-006") }
            }
        }

        @Test
        @DisplayName("이미 예약된 쿠폰이 있으면 서비스가 던진 COUPON-007을 409로 반환한다")
        fun reservationExists() {
            every {
                couponService.redeem(1L, "WELCOME-PRO")
            } throws BusinessException(CouponErrorCode.RESERVATION_EXISTS)

            mockMvc.post("/api/auth/coupons/redeem") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "WELCOME-PRO"))
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.code") { value("COUPON-007") }
            }
        }

        @Test
        @DisplayName("사용 상한에 도달했으면 서비스가 던진 COUPON-005를 409로 반환한다")
        fun exhausted() {
            every {
                couponService.redeem(1L, "WELCOME-PRO")
            } throws BusinessException(CouponErrorCode.COUPON_EXHAUSTED)

            mockMvc.post("/api/auth/coupons/redeem") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("code" to "WELCOME-PRO"))
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.code") { value("COUPON-005") }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/coupons")
    inner class GetMyCoupons {

        @Test
        @DisplayName("내 쿠폰 목록을 200으로 반환한다")
        fun success() {
            every { couponService.getMyCoupons(1L) } returns listOf(
                MyCouponResponse(
                    publicId = "public-1",
                    couponName = "가입 축하 PRO 1개월",
                    grantTier = PlanTier.PRO,
                    status = UserCouponStatus.REDEEMED,
                    redeemedAt = LocalDateTime.now()
                )
            )

            mockMvc.get("/api/auth/coupons") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].couponName") { value("가입 축하 PRO 1개월") }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.get("/api/auth/coupons").andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
