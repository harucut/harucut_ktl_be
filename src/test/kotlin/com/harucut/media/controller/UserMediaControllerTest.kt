package com.harucut.media.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.media.dto.UserMediaResponse
import com.harucut.media.service.UserMediaService
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.util.response.PageResponse
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(UserMediaController::class)
@Import(SecurityConfig::class)
class UserMediaControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var userMediaService: UserMediaService

    private fun authToken(): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "test@harucut.com"
        every { principal.publicId } returns "pub-1"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    private fun response() = UserMediaResponse(
        mediaId = 1L,
        s3Key = "uploads/users/pub-1/fourcuts/x.png",
        displayName = "x.png",
        downloadUrl = "https://dl",
        createdAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("POST /api/auth/user/media")
    inner class Register {

        @Test
        @DisplayName("정상 등록 시 200과 미디어 응답을 반환한다")
        fun success() {
            every { userMediaService.registerMedia(1L, any()) } returns response()
            val body = mapOf(
                "s3Key" to "uploads/users/pub-1/fourcuts/x.png",
                "displayName" to "x"
            )

            mockMvc.post("/api/auth/user/media") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.mediaId") { value(1) }
            }

            verify { userMediaService.registerMedia(1L, any()) }
        }

        @Test
        @DisplayName("s3Key가 비어있으면 400을 반환한다")
        fun validationFail() {
            val body = mapOf("s3Key" to "")

            mockMvc.post("/api/auth/user/media") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userMediaService.registerMedia(any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/user/media") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/media")
    inner class GetMyMedia {

        @Test
        @DisplayName("목록을 200으로 반환한다")
        fun success() {
            every { userMediaService.getMyMedia(eq(1L), any(), any()) } returns
                PageResponse(listOf(response()), 1, 1, 0, 10)

            mockMvc.get("/api/auth/user/media") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].mediaId") { value(1) }
                jsonPath("$.data.totalElements") { value(1) }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/media/{mediaId}/download-url")
    inner class GetDownloadUrl {

        @Test
        @DisplayName("다운로드 URL을 200으로 반환한다")
        fun success() {
            every { userMediaService.getDownloadUrl(1L, 5L) } returns "https://dl"

            mockMvc.get("/api/auth/user/media/5/download-url") {
                with(authentication(authToken()))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data") { value("https://dl") }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/auth/user/media/{mediaId}/display-name")
    inner class UpdateDisplayName {

        @Test
        @DisplayName("정상 수정 시 200을 반환한다")
        fun success() {
            every { userMediaService.updateDisplayName(1L, 5L, any()) } returns response()

            mockMvc.patch("/api/auth/user/media/5/display-name") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("displayName" to "새이름"))
            }.andExpect {
                status { isOk() }
            }

            verify { userMediaService.updateDisplayName(1L, 5L, any()) }
        }

        @Test
        @DisplayName("표시명이 비어있으면 400을 반환한다")
        fun validationFail() {
            mockMvc.patch("/api/auth/user/media/5/display-name") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("displayName" to ""))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { userMediaService.updateDisplayName(any(), any(), any()) }
        }
    }
}
