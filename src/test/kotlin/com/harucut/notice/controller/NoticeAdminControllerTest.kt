package com.harucut.notice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.notice.dto.NoticeAdminResponse
import com.harucut.notice.exception.NoticeErrorCode
import com.harucut.notice.service.NoticeAdminService
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.util.response.PageResponse
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

@WebMvcTest(NoticeAdminController::class)
@Import(SecurityConfig::class)
class NoticeAdminControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var noticeAdminService: NoticeAdminService

    private fun authToken(role: String): UsernamePasswordAuthenticationToken {
        val principal = mockk<CustomUserPrincipal>()
        every { principal.id } returns 1L
        every { principal.username } returns "admin@harucut.com"
        return UsernamePasswordAuthenticationToken(
            principal, null, listOf(SimpleGrantedAuthority(role))
        )
    }

    @Nested
    @DisplayName("POST /api/admin/notices")
    inner class CreateNotice {

        @Test
        @DisplayName("관리자가 공지를 생성하면 200을 반환한다")
        fun success() {
            every { noticeAdminService.createNotice("제목", "본문", false) } just Runs

            mockMvc.post("/api/admin/notices") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("title" to "제목", "content" to "본문", "pinned" to false)
                )
            }.andExpect {
                status { isOk() }
            }

            verify { noticeAdminService.createNotice("제목", "본문", false) }
        }

        @Test
        @DisplayName("제목이 없으면 400을 반환한다")
        fun invalid() {
            mockMvc.post("/api/admin/notices") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("content" to "본문"))
            }.andExpect {
                status { isBadRequest() }
            }

            verify(exactly = 0) { noticeAdminService.createNotice(any(), any(), any()) }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.post("/api/admin/notices") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("title" to "제목", "content" to "본문", "pinned" to false)
                )
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { noticeAdminService.createNotice(any(), any(), any()) }
        }

        @Test
        @DisplayName("미인증이면 401을 반환한다")
        fun unauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/admin/notices") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("title" to "제목", "content" to "본문", "pinned" to false)
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/notices/{noticeId}")
    inner class UpdateNotice {

        @Test
        @DisplayName("관리자가 공지를 수정하면 200을 반환한다")
        fun success() {
            every { noticeAdminService.updateNotice(1L, "새 제목", "새 본문", true) } just Runs

            mockMvc.patch("/api/admin/notices/1") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("title" to "새 제목", "content" to "새 본문", "pinned" to true)
                )
            }.andExpect {
                status { isOk() }
            }

            verify { noticeAdminService.updateNotice(1L, "새 제목", "새 본문", true) }
        }

        @Test
        @DisplayName("존재하지 않는 공지면 서비스가 던진 NOTICE-001을 404로 반환한다")
        fun notFound() {
            every {
                noticeAdminService.updateNotice(1L, "제목", "본문", false)
            } throws BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND)

            mockMvc.patch("/api/admin/notices/1") {
                with(authentication(authToken("ROLE_ADMIN")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("title" to "제목", "content" to "본문", "pinned" to false)
                )
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOTICE-001") }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.patch("/api/admin/notices/1") {
                with(authentication(authToken("ROLE_USER")))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("title" to "제목", "content" to "본문", "pinned" to false)
                )
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/notices/{noticeId}/publish")
    inner class PublishNotice {

        @Test
        @DisplayName("관리자가 공지를 게시하면 200을 반환한다")
        fun success() {
            every { noticeAdminService.publishNotice(1L) } just Runs

            mockMvc.patch("/api/admin/notices/1/publish") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
            }

            verify { noticeAdminService.publishNotice(1L) }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.patch("/api/admin/notices/1/publish") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/notices/{noticeId}/unpublish")
    inner class UnpublishNotice {

        @Test
        @DisplayName("관리자가 공지를 게시 취소하면 200을 반환한다")
        fun success() {
            every { noticeAdminService.unpublishNotice(1L) } just Runs

            mockMvc.patch("/api/admin/notices/1/unpublish") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
            }

            verify { noticeAdminService.unpublishNotice(1L) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/notices/{noticeId}")
    inner class DeleteNotice {

        @Test
        @DisplayName("관리자가 공지를 삭제하면 200을 반환한다")
        fun success() {
            every { noticeAdminService.deleteNotice(1L) } just Runs

            mockMvc.delete("/api/admin/notices/1") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
            }

            verify { noticeAdminService.deleteNotice(1L) }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.delete("/api/admin/notices/1") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }

            verify(exactly = 0) { noticeAdminService.deleteNotice(any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/admin/notices")
    inner class GetAllNotices {

        @Test
        @DisplayName("관리자가 미게시 포함 전체 공지 목록을 200으로 조회한다")
        fun success() {
            every { noticeAdminService.listAllNotices(0, 10) } returns PageResponse(
                content = listOf(
                    NoticeAdminResponse(
                        noticeId = 1L,
                        publicId = "public-1",
                        title = "공지",
                        content = "본문",
                        pinned = false,
                        published = false,
                        publishedAt = null
                    )
                ),
                totalElements = 1,
                totalPages = 1,
                number = 0,
                size = 10
            )

            mockMvc.get("/api/admin/notices") {
                with(authentication(authToken("ROLE_ADMIN")))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].title") { value("공지") }
                jsonPath("$.data.content[0].published") { value(false) }
            }
        }

        @Test
        @DisplayName("ROLE_USER면 403을 반환한다")
        fun forbidden() {
            mockMvc.get("/api/admin/notices") {
                with(authentication(authToken("ROLE_USER")))
            }.andExpect {
                status { isForbidden() }
            }
        }
    }
}
