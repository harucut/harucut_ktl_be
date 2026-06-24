package com.harucut.frame.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.enums.FrameType
import com.harucut.frame.service.FrameService
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@WebMvcTest(FrameController::class)
@Import(SecurityConfig::class)
class FrameControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var frameService: FrameService

    // 인증 principal(id=1) 토큰
    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        every { principal.publicId } returns "pub-1"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    // 유효한 프레임 생성/수정 요청 본문
    private fun requestBody(title: String = "봄 여행 4컷") = mapOf(
        "title" to title,
        "description" to "벚꽃",
        "previewKey" to "temp/users/pub-1/preview.png",
        "frameType" to "CLASSIC",
        "background" to mapOf("type" to "COLOR", "value" to "#ffffff"),
        "components" to emptyList<Any>()
    )

    private fun frameResponse() = FrameResponse(
        frameId = 1L,
        title = "봄 여행 4컷",
        description = "벚꽃",
        source = "https://preview",
        frameType = FrameType.CLASSIC,
        background = ColorBackgroundAttributes("#ffffff"),
        components = emptyList()
    )

    @Nested
    @DisplayName("POST /api/auth/user/frame")
    inner class Create {

        @Test
        @DisplayName("정상 생성 시 200을 반환한다")
        fun success() {
            every { frameService.createFrame(1L, any()) } just Runs

            mockMvc.post("/api/auth/user/frame") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isOk() }
            }

            verify { frameService.createFrame(1L, any()) }
        }

        @Test
        @DisplayName("제목이 비어있으면 400을 반환한다")
        fun validationFail() {
            mockMvc.post("/api/auth/user/frame") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody(title = ""))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { frameService.createFrame(any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/user/frame") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/frame")
    inner class GetMyFrames {

        @Test
        @DisplayName("목록을 200으로 반환한다")
        fun success() {
            every { frameService.getMyFrames(1L) } returns listOf(frameResponse())

            mockMvc.get("/api/auth/user/frame") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].frameId") { value(1) }
                jsonPath("$.data[0].background.type") { value("COLOR") }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/frame/{frameId}")
    inner class GetFrame {

        @Test
        @DisplayName("단건을 200으로 반환한다")
        fun success() {
            every { frameService.getFrame(5L, 1L) } returns frameResponse()

            mockMvc.get("/api/auth/user/frame/5") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.frameId") { value(1) }
                jsonPath("$.data.source") { value("https://preview") }
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/user/frame/{frameId}")
    inner class Update {

        @Test
        @DisplayName("정상 수정 시 200을 반환한다")
        fun success() {
            every { frameService.updateFrame(1L, 5L, any()) } just Runs

            mockMvc.put("/api/auth/user/frame/5") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isOk() }
            }

            verify { frameService.updateFrame(1L, 5L, any()) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/auth/user/frame/{frameId}")
    inner class Delete {

        @Test
        @DisplayName("정상 삭제 시 200을 반환한다")
        fun success() {
            every { frameService.deleteFrame(1L, 5L) } just Runs

            mockMvc.delete("/api/auth/user/frame/5") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
            }

            verify { frameService.deleteFrame(1L, 5L) }
        }
    }
}
