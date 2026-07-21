package com.harucut.terms.controller

import com.harucut.config.SecurityConfig
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.terms.dto.TermsResponse
import com.harucut.terms.service.TermsService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(TermsController::class)
@Import(SecurityConfig::class)
class TermsControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var termsService: TermsService

    @Nested
    @DisplayName("GET /api/terms")
    inner class GetTerms {

        @Test
        @DisplayName("토큰 없이도 활성 약관 목록을 200으로 반환한다")
        fun success() {
            every { termsService.getActiveTerms() } returns listOf(
                TermsResponse(code = "tos", title = "이용약관", required = true, version = 1, content = "본문")
            )

            mockMvc.get("/api/terms").andExpect {
                status { isOk() }
                jsonPath("$.data[0].code") { value("tos") }
                jsonPath("$.data[0].version") { value(1) }
            }
        }
    }
}
