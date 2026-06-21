package com.harucut.media.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.config.SecurityConfig
import com.harucut.media.service.TranscodingService
import com.harucut.support.SecurityBeansMockSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.client.RestClient

@WebMvcTest(MediaConvertWebhookController::class)
@Import(SecurityConfig::class)
class MediaConvertWebhookControllerTest : SecurityBeansMockSupport() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var transcodingService: TranscodingService

    @MockkBean(relaxed = true)
    lateinit var restClient: RestClient

    private fun snsNotification(detail: Map<String, Any?>): String {
        val message = objectMapper.writeValueAsString(mapOf("detail" to detail))
        return objectMapper.writeValueAsString(mapOf("Type" to "Notification", "Message" to message))
    }

    private fun postWebhook(body: String) =
        mockMvc.post("/api/webhooks/mediaconvert") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    @Nested
    @DisplayName("POST /api/webhooks/mediaconvert (인증 없이 접근 가능)")
    inner class Webhook {

        @Test
        @DisplayName("완료 알림이면 mp4·썸네일 경로를 분류해 완료 처리한다")
        fun complete() {
            every { transcodingService.handleCompletedJob(any(), any(), any(), any(), any()) } just runs
            val detail = mapOf(
                "jobId" to "job-1",
                "status" to "COMPLETE",
                "userMetadata" to mapOf("userPublicId" to "pub-1", "originalFileName" to "v.webm"),
                "outputGroupDetails" to listOf(
                    mapOf("outputDetails" to listOf(mapOf("outputFilePaths" to listOf("s3://b/uploads/users/pub-1/mp4/v.mp4")))),
                    mapOf("outputDetails" to listOf(mapOf("outputFilePaths" to listOf("s3://b/uploads/users/pub-1/thumbnail/v.0000000.jpg"))))
                )
            )

            postWebhook(snsNotification(detail)).andExpect { status { isOk() } }

            verify {
                transcodingService.handleCompletedJob(
                    "job-1", "pub-1", "v.webm",
                    "s3://b/uploads/users/pub-1/mp4/v.mp4",
                    "s3://b/uploads/users/pub-1/thumbnail/v.0000000.jpg"
                )
            }
        }

        @Test
        @DisplayName("완료지만 mp4 경로가 없으면 실패 처리한다")
        fun completeWithoutMp4() {
            every { transcodingService.handleFailedJob(any(), any()) } just runs
            val detail = mapOf(
                "jobId" to "job-1",
                "status" to "COMPLETE",
                "userMetadata" to mapOf("userPublicId" to "pub-1", "originalFileName" to "v.webm"),
                "outputGroupDetails" to listOf(
                    mapOf("outputDetails" to listOf(mapOf("outputFilePaths" to listOf("s3://b/uploads/users/pub-1/thumbnail/v.0000000.jpg"))))
                )
            )

            postWebhook(snsNotification(detail)).andExpect { status { isOk() } }

            verify { transcodingService.handleFailedJob("job-1", any()) }
            verify(exactly = 0) { transcodingService.handleCompletedJob(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("진행 알림이면 진행 처리한다")
        fun progressing() {
            every { transcodingService.markProgressing(any()) } just runs
            val detail = mapOf("jobId" to "job-1", "status" to "PROGRESSING")

            postWebhook(snsNotification(detail)).andExpect { status { isOk() } }

            verify { transcodingService.markProgressing("job-1") }
        }

        @Test
        @DisplayName("에러 알림이면 메시지와 함께 실패 처리한다")
        fun error() {
            every { transcodingService.handleFailedJob(any(), any()) } just runs
            val detail = mapOf("jobId" to "job-1", "status" to "ERROR", "errorMessage" to "boom")

            postWebhook(snsNotification(detail)).andExpect { status { isOk() } }

            verify { transcodingService.handleFailedJob("job-1", "boom") }
        }

        @Test
        @DisplayName("SNS 구독 확인 요청이면 SubscribeURL로 확인 호출한다")
        fun subscriptionConfirmation() {
            val body = objectMapper.writeValueAsString(
                mapOf("Type" to "SubscriptionConfirmation", "SubscribeURL" to "https://sns/confirm")
            )

            postWebhook(body).andExpect { status { isOk() } }

            verify { restClient.get() }
        }
    }
}
