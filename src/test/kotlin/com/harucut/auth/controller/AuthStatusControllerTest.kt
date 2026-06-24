package com.harucut.auth.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.user.enums.UserStatus
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(AuthStatusController::class)
@Import(SecurityConfig::class)
class AuthStatusControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private fun authToken(status: UserStatus = UserStatus.ACTIVE): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        every { principal.publicId } returns "pub-1"
        every { principal.status } returns status
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @Test
    @DisplayName("GET /api/auth/status — 로그인 사용자의 상태를 200으로 반환한다")
    fun success() {
        mockMvc.get("/api/auth/status") {
            with(authentication(authToken(UserStatus.ACTIVE)))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.userStatus") { value("ACTIVE") }
        }
    }

    @Test
    @DisplayName("GET /api/auth/status — 미인증이면 401을 반환한다")
    fun unauthorized() {
        every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
            secondArg<HttpServletResponse>().sendError(401)
        }

        mockMvc.get("/api/auth/status").andExpect {
            status { isUnauthorized() }
        }
    }
}
