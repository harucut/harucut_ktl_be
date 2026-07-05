package com.harucut.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.subscription.plan.PlanTier
import com.harucut.user.dto.SubscriptionUsageResponse
import com.harucut.user.dto.UserInfoResponse
import com.harucut.user.service.UserService
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

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var userService: UserService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        every { principal.publicId } returns "pub-1"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    private fun userInfo() = UserInfoResponse(
        id = 1L,
        email = "user@harucut.com",
        username = "하루컷",
        profileUrl = "https://profile",
        loginPlatform = "HARUCUT",
        planTier = "PLUS",
        monthlyPrice = 3900
    )

    private fun usage() = SubscriptionUsageResponse(
        planTier = "PLUS",
        frameRetentionLimit = 3,
        frameRetentionUsedCount = 1,
        frameRetentionRemainingCount = 2,
        frameRetentionUnlimited = false
    )

    @Nested
    @DisplayName("GET /api/auth/user/info")
    inner class GetUserInfo {

        @Test
        @DisplayName("내 정보를 200으로 반환한다")
        fun success() {
            every { userService.getUserInfo(1L) } returns userInfo()

            mockMvc.get("/api/auth/user/info") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.planTier") { value("PLUS") }
                jsonPath("$.data.monthlyPrice") { value(3900) }
            }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.get("/api/auth/user/info").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/subscription/usage")
    inner class GetSubscriptionUsage {

        @Test
        @DisplayName("구독 사용량을 200으로 반환한다")
        fun success() {
            every { userService.getSubscriptionUsage(1L) } returns usage()

            mockMvc.get("/api/auth/user/subscription/usage") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.frameRetentionUsedCount") { value(1) }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/auth/user/subscription/plan")
    inner class ChangePlan {

        @Test
        @DisplayName("planTier로 요금제를 변경하고 200을 반환한다")
        fun success() {
            every { userService.changePlan(1L, PlanTier.PLUS) } just Runs

            mockMvc.patch("/api/auth/user/subscription/plan") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("planTier" to "PLUS"))
            }.andExpect {
                status { isOk() }
            }

            verify { userService.changePlan(1L, PlanTier.PLUS) }
        }

        @Test
        @DisplayName("존재하지 않는 요금제 값이면 400을 반환한다")
        fun invalidTier() {
            mockMvc.patch("/api/auth/user/subscription/plan") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("planTier" to "MEGA"))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userService.changePlan(any(), any()) }
        }

        @Test
        @DisplayName("planTier가 없으면 400을 반환한다")
        fun missingTier() {
            mockMvc.patch("/api/auth/user/subscription/plan") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userService.changePlan(any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.patch("/api/auth/user/subscription/plan") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("planTier" to "PLUS"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/auth/user/change/username")
    inner class ChangeUsername {

        @Test
        @DisplayName("username 파라미터로 닉네임을 변경하고 200을 반환한다")
        fun success() {
            every { userService.changeUsername(1L, "새이름") } just Runs

            mockMvc.patch("/api/auth/user/change/username") {
                with(authentication(authToken()))
                param("username", "새이름")
            }.andExpect {
                status { isOk() }
            }

            verify { userService.changeUsername(1L, "새이름") }
        }

        @Test
        @DisplayName("username 파라미터가 없으면 400을 반환한다")
        fun missingParam() {
            mockMvc.patch("/api/auth/user/change/username") {
                with(authentication(authToken()))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userService.changeUsername(any(), any()) }
        }

        @Test
        @DisplayName("username이 공백이면 400을 반환한다(@NotBlank)")
        fun blankUsername() {
            mockMvc.patch("/api/auth/user/change/username") {
                with(authentication(authToken()))
                param("username", "   ")
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userService.changeUsername(any(), any()) }
        }

        @Test
        @DisplayName("username이 20자를 초과하면 400을 반환한다(@Size)")
        fun tooLongUsername() {
            mockMvc.patch("/api/auth/user/change/username") {
                with(authentication(authToken()))
                param("username", "a".repeat(21))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userService.changeUsername(any(), any()) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/auth/user/change/profile-image")
    inner class ChangeProfileImage {

        @Test
        @DisplayName("s3Key로 프로필 이미지를 변경하고 200을 반환한다")
        fun success() {
            every { userService.changeProfileImage(1L, "uploads/users/pub-1/profile/new.png") } just Runs

            mockMvc.patch("/api/auth/user/change/profile-image") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("s3Key" to "uploads/users/pub-1/profile/new.png"))
            }.andExpect {
                status { isOk() }
            }

            verify { userService.changeProfileImage(1L, "uploads/users/pub-1/profile/new.png") }
        }

        @Test
        @DisplayName("s3Key가 비어있으면 400을 반환한다")
        fun validationFail() {
            mockMvc.patch("/api/auth/user/change/profile-image") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("s3Key" to ""))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userService.changeProfileImage(any(), any()) }
        }
    }
}
