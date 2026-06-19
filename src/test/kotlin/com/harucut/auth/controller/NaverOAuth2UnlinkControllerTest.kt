package com.harucut.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.oauth2.service.NaverOAuth2UnlinkService
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(NaverOAuth2UnlinkController::class)
@Import(SecurityConfig::class)
class NaverOAuth2UnlinkControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var naverOAuth2UnlinkService: NaverOAuth2UnlinkService

    private val request = NaverUnlinkRequest("cid", "enc", "ts", "sig")

    @Test
    @DisplayName("인증 없이 호출 가능하며 성공 시 204 를 반환한다")
    fun success() {
        // given
        every { naverOAuth2UnlinkService.unlink(any()) } just runs

        // when & then
        mockMvc.post("/api/oauth2/unlink/naver") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @DisplayName("서명 검증 실패 등 unlink 예외 시 500 을 반환한다")
    fun unlinkFailed() {
        // given
        every { naverOAuth2UnlinkService.unlink(any()) } throws
                BusinessException(AuthErrorCode.OAUTH2_UNLINK_FAILED)

        // when & then
        mockMvc.post("/api/oauth2/unlink/naver") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isInternalServerError() }
        }
    }
}