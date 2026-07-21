package com.harucut.terms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.terms.dto.TermsAdminResponse
import com.harucut.terms.exception.TermsErrorCode
import com.harucut.terms.service.TermsAdminService
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(TermsAdminController::class)
@Import(SecurityConfig::class)
class TermsAdminControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var termsAdminService: TermsAdminService

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "admin@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    @Nested
    @DisplayName("POST /api/admin/terms")
    inner class CreateTerms {

        @Test
        @DisplayName("관리자가 약관을 생성하면 200을 반환한다")
        fun success() {
            every { termsAdminService.createTerms("tos", "이용약관", true, "본문") } just Runs

            mockMvc.post("/api/admin/terms") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("code" to "tos", "title" to "이용약관", "required" to true, "content" to "본문")
                )
            }.andExpect {
                status { isOk() }
            }

            verify { termsAdminService.createTerms("tos", "이용약관", true, "본문") }
        }

        @Test
        @DisplayName("코드 형식이 잘못되면 400을 반환한다")
        fun invalidCode() {
            mockMvc.post("/api/admin/terms") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("code" to "Invalid Code!", "title" to "이용약관", "required" to true, "content" to "본문")
                )
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { termsAdminService.createTerms(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("중복 코드면 서비스가 던진 TERMS-002를 409로 반환한다")
        fun duplicated() {
            every {
                termsAdminService.createTerms("tos", "이용약관", true, "본문")
            } throws BusinessException(TermsErrorCode.TERMS_CODE_DUPLICATED)

            mockMvc.post("/api/admin/terms") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("code" to "tos", "title" to "이용약관", "required" to true, "content" to "본문")
                )
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.code") { value("TERMS-002") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.post("/api/admin/terms") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("code" to "tos", "title" to "이용약관", "required" to true, "content" to "본문")
                )
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { termsAdminService.createTerms(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/admin/terms") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("code" to "tos", "title" to "이용약관", "required" to true, "content" to "본문")
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/admin/terms/{termsId}/versions")
    inner class ReviseTerms {

        @Test
        @DisplayName("관리자가 약관을 개정하면 200을 반환한다")
        fun success() {
            every { termsAdminService.reviseTerms(1L, "개정본") } just Runs

            mockMvc.post("/api/admin/terms/1/versions") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("content" to "개정본"))
            }.andExpect {
                status { isOk() }
            }

            verify { termsAdminService.reviseTerms(1L, "개정본") }
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 서비스가 던진 TERMS-001을 404로 반환한다")
        fun notFound() {
            every {
                termsAdminService.reviseTerms(1L, "개정본")
            } throws BusinessException(TermsErrorCode.TERMS_NOT_FOUND)

            mockMvc.post("/api/admin/terms/1/versions") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("content" to "개정본"))
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("TERMS-001") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.post("/api/admin/terms/1/versions") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("content" to "개정본"))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/admin/terms")
    inner class GetAllTerms {

        @Test
        @DisplayName("관리자가 전체 약관 목록을 200으로 조회한다")
        fun success() {
            every { termsAdminService.listAllTerms() } returns listOf(
                TermsAdminResponse(
                    termsId = 1L,
                    code = "tos",
                    title = "이용약관",
                    required = true,
                    active = true,
                    latestVersion = 1,
                    content = "본문"
                )
            )

            mockMvc.get("/api/admin/terms") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].code") { value("tos") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/terms") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/terms/{termsId}")
    inner class DeactivateTerms {

        @Test
        @DisplayName("관리자가 약관을 비활성화하면 200을 반환한다")
        fun success() {
            every { termsAdminService.deactivateTerms(1L) } just Runs

            mockMvc.delete("/api/admin/terms/1") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
            }

            verify { termsAdminService.deactivateTerms(1L) }
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 서비스가 던진 TERMS-001을 404로 반환한다")
        fun notFound() {
            every { termsAdminService.deactivateTerms(1L) } throws BusinessException(TermsErrorCode.TERMS_NOT_FOUND)

            mockMvc.delete("/api/admin/terms/1") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isNotFound() }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.delete("/api/admin/terms/1") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { termsAdminService.deactivateTerms(any()) }
        }
    }
}
