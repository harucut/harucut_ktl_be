package com.harucut.frame.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.dto.FrameResponse
import com.harucut.frame.enums.FrameType
import com.harucut.frame.exception.FrameErrorCode
import com.harucut.frame.service.FrameAdminService
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@WebMvcTest(FrameAdminController::class)
@Import(SecurityConfig::class)
class FrameAdminControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var frameAdminService: FrameAdminService

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "admin@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    private fun requestBody(title: String = "기본 프레임") = mapOf(
        "title" to title,
        "description" to "설명",
        "previewKey" to "uploads/system/preview.png",
        "frameType" to "CLASSIC",
        "background" to mapOf("type" to "COLOR", "value" to "#ffffff"),
        "components" to emptyList<Any>()
    )

    private fun frameResponse() = FrameResponse(
        frameId = 1L,
        title = "기본 프레임",
        description = "설명",
        source = "https://preview",
        frameType = FrameType.CLASSIC,
        background = ColorBackgroundAttributes("#ffffff"),
        components = emptyList(),
        isSystem = true
    )

    @Nested
    @DisplayName("POST /api/admin/frames")
    inner class CreateFrame {

        @Test
        @DisplayName("관리자가 시스템 프레임을 생성하면 200을 반환한다")
        fun success() {
            every { frameAdminService.createSystemFrame(any()) } just Runs

            mockMvc.post("/api/admin/frames") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isOk() }
            }

            verify { frameAdminService.createSystemFrame(any()) }
        }

        @Test
        @DisplayName("제목이 없으면 400을 반환한다")
        fun invalid() {
            mockMvc.post("/api/admin/frames") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody(title = ""))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { frameAdminService.createSystemFrame(any()) }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.post("/api/admin/frames") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { frameAdminService.createSystemFrame(any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/admin/frames") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/frames/{frameId}")
    inner class UpdateFrame {

        @Test
        @DisplayName("관리자가 시스템 프레임을 수정하면 200을 반환한다")
        fun success() {
            every { frameAdminService.updateSystemFrame(1L, any()) } just Runs

            mockMvc.patch("/api/admin/frames/1") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody(title = "수정된 제목"))
            }.andExpect {
                status { isOk() }
            }

            verify { frameAdminService.updateSystemFrame(1L, any()) }
        }

        @Test
        @DisplayName("존재하지 않는 시스템 프레임이면 FRAME-001을 404로 반환한다")
        fun notFound() {
            every {
                frameAdminService.updateSystemFrame(1L, any())
            } throws BusinessException(FrameErrorCode.SYSTEM_FRAME_NOT_FOUND)

            mockMvc.patch("/api/admin/frames/1") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("FRAME-001") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.patch("/api/admin/frames/1") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody())
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/frames/{frameId}")
    inner class DeleteFrame {

        @Test
        @DisplayName("관리자가 시스템 프레임을 삭제하면 200을 반환한다")
        fun success() {
            every { frameAdminService.deleteSystemFrame(1L) } just Runs

            mockMvc.delete("/api/admin/frames/1") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
            }

            verify { frameAdminService.deleteSystemFrame(1L) }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.delete("/api/admin/frames/1") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { frameAdminService.deleteSystemFrame(any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/admin/frames")
    inner class GetSystemFrames {

        @Test
        @DisplayName("관리자가 시스템 프레임 목록을 200으로 조회한다")
        fun success() {
            every { frameAdminService.listSystemFrames() } returns listOf(frameResponse())

            mockMvc.get("/api/admin/frames") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].title") { value("기본 프레임") }
                jsonPath("$.data[0].isSystem") { value(true) }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/frames") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }
}
