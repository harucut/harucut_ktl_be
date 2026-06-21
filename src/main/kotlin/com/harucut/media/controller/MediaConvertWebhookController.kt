package com.harucut.media.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.media.dto.AwsSnsMessage
import com.harucut.media.service.TranscodingService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import java.net.URI

@RestController
@RequestMapping("/api/webhooks/mediaconvert")
class MediaConvertWebhookController(
    private val objectMapper: ObjectMapper,
    private val transcodingService: TranscodingService,
    private val restClient: RestClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // SNS 알림 수신 (구독확인 / 완료·진행·실패 분기)
    @PostMapping
    fun handleMediaConvertNotification(@RequestBody rawBody: String) {
        try {
            val snsMessage = objectMapper.readValue(rawBody, AwsSnsMessage::class.java)
            when (snsMessage.type) {
                "SubscriptionConfirmation" -> {
                    log.info("AWS SNS 구독 확인 요청 도착")
                    snsMessage.subscribeUrl?.let {
                        restClient.get().uri(URI.create(it)).retrieve().body(String::class.java)
                    }
                    log.info("AWS SNS 구독 승인 완료")
                }

                "Notification" -> snsMessage.message?.let { processConversionResult(it) }
            }
        } catch (e: Exception) {
            log.error("웹훅 처리 중 오류 발생. error={}", e.message, e)
        }
    }

    // MediaConvert 이벤트 파싱 후 상태별 처리
    private fun processConversionResult(messageJson: String) {
        val detail = objectMapper.readTree(messageJson).path("detail")
        val jobId = detail.path("jobId").asText()
        val state = detail.path("status").asText("UNKNOWN")

        when (state) {
            "COMPLETE" -> {
                val userMetadata = detail.path("userMetadata")
                val userPublicId = userMetadata.path("userPublicId").asText(null)
                val originalFileName = userMetadata.path("originalFileName").asText(null)

                val outputPaths = collectOutputPaths(detail)
                val mp4Path = outputPaths.firstOrNull { it.endsWith(".mp4", ignoreCase = true) }
                val thumbnailPath = outputPaths.firstOrNull {
                    it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true)
                }

                if (mp4Path == null) {
                    log.error("변환 완료지만 mp4 출력 경로 없음. jobId={}, paths={}", jobId, outputPaths)
                    transcodingService.handleFailedJob(jobId, "No mp4 output path in completion event")
                    return
                }

                log.info("변환 완료. jobId={}, mp4={}, thumbnail={}", jobId, mp4Path, thumbnailPath)
                try {
                    transcodingService.handleCompletedJob(jobId, userPublicId, originalFileName, mp4Path, thumbnailPath)
                } catch (e: Exception) {
                    log.error("변환 완료 후 DB 저장 실패. jobId={}, error={}", jobId, e.message, e)
                    transcodingService.handleFailedJob(jobId, "Failed to persist transcoded video")
                }
            }

            "PROGRESSING", "STATUS_UPDATE" -> transcodingService.markProgressing(jobId)

            "ERROR" -> {
                val errorMessage = detail.path("errorMessage").asText("Unknown Error")
                log.error("변환 실패 jobId={}, message={}", jobId, errorMessage)
                transcodingService.handleFailedJob(jobId, errorMessage)
            }
        }
    }

    // 출력 그룹에서 모든 출력 파일 경로 수집
    private fun collectOutputPaths(detail: JsonNode): List<String> {
        val paths = mutableListOf<String>()
        detail.path("outputGroupDetails").forEach { og ->
            og.path("outputDetails").forEach { od ->
                od.path("outputFilePaths").forEach { p ->
                    p.asText("").takeIf { it.isNotBlank() }?.let { paths.add(it) }
                }
            }
        }
        return paths
    }
}