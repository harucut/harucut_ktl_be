package com.harucut.notice.controller

import com.harucut.config.SecurityConfig
import com.harucut.exception.BusinessException
import com.harucut.notice.dto.NoticeResponse
import com.harucut.notice.exception.NoticeErrorCode
import com.harucut.notice.service.NoticeService
import com.harucut.support.SecurityBeansMockSupport
import com.harucut.util.response.PageResponse
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(NoticeController::class)
@Import(SecurityConfig::class)
class NoticeControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var noticeService: NoticeService

    @Nested
    @DisplayName("GET /api/notices")
    inner class GetNotices {

        @Test
        @DisplayName("토큰 없이도 게시된 공지 목록을 200으로 반환한다")
        fun success() {
            every { noticeService.getPublishedNotices(0, 10) } returns PageResponse(
                content = listOf(
                    NoticeResponse(
                        publicId = "public-1",
                        title = "공지",
                        content = "본문",
                        pinned = true,
                        publishedAt = null
                    )
                ),
                totalElements = 1,
                totalPages = 1,
                number = 0,
                size = 10
            )

            mockMvc.get("/api/notices").andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].title") { value("공지") }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/notices/{publicId}")
    inner class GetNotice {

        @Test
        @DisplayName("토큰 없이도 게시된 공지 단건을 200으로 반환한다")
        fun success() {
            every { noticeService.getPublishedNotice("public-1") } returns NoticeResponse(
                publicId = "public-1",
                title = "공지",
                content = "본문",
                pinned = false,
                publishedAt = null
            )

            mockMvc.get("/api/notices/public-1").andExpect {
                status { isOk() }
                jsonPath("$.data.title") { value("공지") }
            }
        }

        @Test
        @DisplayName("미게시/존재하지 않는 공지면 404를 반환한다")
        fun notFound() {
            every {
                noticeService.getPublishedNotice("unknown")
            } throws BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND)

            mockMvc.get("/api/notices/unknown").andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOTICE-001") }
            }
        }
    }

    @Nested
    @DisplayName("/api/notices 공개 범위는 GET으로 제한된다")
    inner class NonGetAccess {

        @Test
        @DisplayName("인증 없이 POST /api/notices 요청하면 401을 반환한다 (GET만 공개)")
        fun postUnauthorized() {
            every { customAuthenticationEntryPoint.commence(any(), any(), any()) } answers {
                secondArg<HttpServletResponse>().sendError(401)
            }

            mockMvc.post("/api/notices") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
