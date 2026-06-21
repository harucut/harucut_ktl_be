package com.harucut.media.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.media.dto.TranscodeTaskStatusResponse
import com.harucut.media.dto.TranscodeTaskSubmitResponse
import com.harucut.media.enums.TranscodeTaskStatus
import com.harucut.media.service.TranscodingService
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

@WebMvcTest(TranscodeController::class)
@Import(SecurityConfig::class)
class TranscodeControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var transcodingService: TranscodingService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        every { principal.publicId } returns "pub-1"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @Nested
    @DisplayName("POST /api/auth/user/files/transcode")
    inner class StartTranscoding {

        @Test
        @DisplayName("정상 제출 시 202와 taskId/jobId를 반환한다")
        fun success() {
            every { transcodingService.submitTranscodeTask("pub-1", "v.webm") } returns
                TranscodeTaskSubmitResponse("t1", "job-1", TranscodeTaskStatus.SUBMITTED, LocalDateTime.now())

            mockMvc.post("/api/auth/user/files/transcode") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("filename" to "v.webm"))
            }.andExpect {
                status { isAccepted() }
                jsonPath("$.data.taskId") { value("t1") }
                jsonPath("$.data.jobId") { value("job-1") }
                jsonPath("$.data.status") { value("SUBMITTED") }
            }

            verify { transcodingService.submitTranscodeTask("pub-1", "v.webm") }
        }

        @Test
        @DisplayName("filename이 비어있으면 400을 반환한다")
        fun validationFail() {
            mockMvc.post("/api/auth/user/files/transcode") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("filename" to ""))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { transcodingService.submitTranscodeTask(any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/user/files/transcode") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/files/transcode/status")
    inner class GetStatus {

        @Test
        @DisplayName("상태를 200으로 반환한다")
        fun success() {
            val now = LocalDateTime.now()
            every { transcodingService.getTaskStatus("t1", "pub-1") } returns
                TranscodeTaskStatusResponse("t1", "job-1", TranscodeTaskStatus.COMPLETE, null, null, now, now)

            mockMvc.get("/api/auth/user/files/transcode/status") {
                with(authentication(authToken()))
                param("taskId", "t1")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.taskId") { value("t1") }
                jsonPath("$.data.status") { value("COMPLETE") }
            }
        }
    }
}
