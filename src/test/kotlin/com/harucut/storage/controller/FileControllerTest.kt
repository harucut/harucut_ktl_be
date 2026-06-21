package com.harucut.storage.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.storage.dto.PresignedUploadResponse
import com.harucut.storage.service.FileStorageService
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
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
import java.time.Duration

@WebMvcTest(FileController::class)
@Import(SecurityConfig::class)
class FileControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var fileStorageService: FileStorageService

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
    @DisplayName("POST /api/auth/user/files/presigned-upload")
    inner class PresignedUpload {

        @Test
        @DisplayName("인증된 유저의 정상 요청 시 presigned 정보를 200으로 반환한다")
        fun success() {
            // given
            every {
                fileStorageService.generatePresignedUploadUrl(any(), any(), any(), "pub-1", false)
            } returns PresignedUploadResponse(
                key = "uploads/users/pub-1/profile/x.png",
                uploadUrl = "https://x.s3/upload",
                contentType = "image/png",
                expiresIn = Duration.ofDays(1)
            )
            val body = mapOf(
                "type" to "PROFILE",
                "filename" to "photo.png",
                "contentType" to "PNG",
                "isTemp" to false
            )

            // when & then
            mockMvc.post("/api/auth/user/files/presigned-upload") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.key") { value("uploads/users/pub-1/profile/x.png") }
                jsonPath("$.data.contentType") { value("image/png") }
            }

            verify { fileStorageService.generatePresignedUploadUrl(any(), any(), any(), "pub-1", false) }
        }

        @Test
        @DisplayName("filename이 비어있으면 400을 반환한다")
        fun validationFail() {
            val body = mapOf(
                "type" to "PROFILE",
                "filename" to "",
                "contentType" to "PNG"
            )

            mockMvc.post("/api/auth/user/files/presigned-upload") {
                with(authentication(authToken()))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { fileStorageService.generatePresignedUploadUrl(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("미인증 상태면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/auth/user/files/presigned-upload") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/auth/user/files/presigned-img")
    inner class PresignedImg {

        @Test
        @DisplayName("key에 대한 presigned URL을 200으로 반환한다")
        fun success() {
            every { fileStorageService.generatePresignedGetUrl("uploads/a.png") } returns "https://x.s3/get"

            mockMvc.get("/api/auth/user/files/presigned-img") {
                with(authentication(authToken()))
                param("key", "uploads/a.png")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data") { value("https://x.s3/get") }
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/auth/user/files/delete")
    inner class Delete {

        @Test
        @DisplayName("key로 삭제 요청 시 200을 반환한다")
        fun success() {
            every { fileStorageService.delete("uploads/a.png") } just runs

            mockMvc.delete("/api/auth/user/files/delete") {
                with(authentication(authToken()))
                param("key", "uploads/a.png")
            }.andExpect {
                status { isOk() }
            }

            verify { fileStorageService.delete("uploads/a.png") }
        }
    }
}