package com.harucut.auth.controller

import com.harucut.auth.exit.service.UserExitService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post

@WebMvcTest(UserExitController::class)
@Import(SecurityConfig::class)
class UserExitControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var userExitService: UserExitService

    @MockkBean
    lateinit var cookieManager: CookieManager

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    @Nested
    @DisplayName("DELETE /api/harucut/exit")
    inner class Exit {

        @Test
        @DisplayName("인증된 유저의 탈퇴 요청 시 토큰 쿠키를 만료시키고 200을 반환한다")
        fun success() {
            // given
            every { userExitService.requestExit(1L) } just runs
            every { cookieManager.createExpiredCookie(any()) } returns
                    ResponseCookie.from("expired", "").build()

            // when & then
            mockMvc.delete("/api/harucut/exit") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isOk() }
                header { exists("Set-Cookie") }
            }

            verify { userExitService.requestExit(1L) }
        }

        @Test
        @DisplayName("미인증 상태면 401을 반환한다")
        fun unauthorized() {
            // given
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            // when & then
            mockMvc.delete("/api/harucut/exit").andExpect {
                status { isUnauthorized() }
            }

            verify(exactly = 0) { userExitService.requestExit(any()) }
        }
    }

    @Nested
    @DisplayName("POST /api/harucut/reactivate")
    inner class Reactivate {

        @Test
        @DisplayName("DELETED_REQUESTED 권한 유저의 탈퇴 취소 시 200을 반환한다")
        fun success() {
            // given
            every { userExitService.reActivate(1L) } just runs

            // when & then
            mockMvc.post("/api/harucut/reactivate") {
                with(authentication(authToken("ROLE_DELETED_REQUESTED")))
            }.andExpect {
                status { isOk() }
            }

            verify { userExitService.reActivate(1L) }
        }

        @Test
        @DisplayName("DELETED_REQUESTED 권한이 없는 유저면 403을 반환한다")
        fun forbidden() {
            // when & then
            mockMvc.post("/api/harucut/reactivate") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { userExitService.reActivate(any()) }
        }

        @Test
        @DisplayName("미인증 상태면 401을 반환한다")
        fun unauthorized() {
            // given
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            // when & then
            mockMvc.post("/api/harucut/reactivate").andExpect {
                status { isUnauthorized() }
            }

            verify(exactly = 0) { userExitService.reActivate(any()) }
        }
    }

}