package com.harucut.media.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.harucut.media.enums.TranscodeTaskStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "동영상 변환 요청")
data class TranscodeRequest(
    @field:NotBlank(message = "파일명은 필수입니다.")
    @Schema(description = "S3에 업로드된 UUID 파일명 (확장자 포함)", example = "550e8400-e29b-41d4-a716-446655440000.webm")
    val filename: String
)

@Schema(description = "변환 작업 제출 응답")
data class TranscodeTaskSubmitResponse(
    @Schema(description = "변환 추적 ID", example = "3dc5f9c5-27b5-4926-a887-6e96c9c5195e")
    val taskId: String,
    @Schema(description = "MediaConvert Job ID", example = "1705389394214-abcdef")
    val jobId: String?,
    @Schema(description = "현재 상태", example = "SUBMITTED")
    val status: TranscodeTaskStatus,
    @Schema(description = "요청 시각")
    val requestedAt: LocalDateTime
)

@Schema(description = "변환 작업 상태 조회 응답")
data class TranscodeTaskStatusResponse(
    @Schema(description = "변환 추적 ID")
    val taskId: String,
    @Schema(description = "MediaConvert Job ID")
    val jobId: String?,
    @Schema(description = "현재 상태", example = "COMPLETE")
    val status: TranscodeTaskStatus,
    @Schema(description = "실패 메시지")
    val errorMessage: String?,
    @Schema(description = "완료 시 저장된 사용자 미디어 정보")
    val media: UserMediaResponse?,
    @Schema(description = "작업 생성 시각")
    val createdAt: LocalDateTime,
    @Schema(description = "최종 갱신 시각")
    val updatedAt: LocalDateTime
)

/** Redis에 저장되는 변환 작업 상태 (불변, copy 기반 전이) */
data class TranscodeTaskState(
    val taskId: String,
    val userPublicId: String,
    val originalFileName: String,
    val jobId: String?,
    val status: TranscodeTaskStatus,
    val errorMessage: String?,
    val media: UserMediaResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun submitted(jobId: String, now: LocalDateTime): TranscodeTaskState =
        copy(jobId = jobId, status = TranscodeTaskStatus.SUBMITTED, errorMessage = null, updatedAt = now)

    fun progressing(now: LocalDateTime): TranscodeTaskState =
        copy(status = TranscodeTaskStatus.PROGRESSING, errorMessage = null, updatedAt = now)

    fun completed(media: UserMediaResponse, now: LocalDateTime): TranscodeTaskState =
        copy(status = TranscodeTaskStatus.COMPLETE, errorMessage = null, media = media, updatedAt = now)

    fun failed(message: String, now: LocalDateTime): TranscodeTaskState =
        copy(status = TranscodeTaskStatus.ERROR, errorMessage = message, updatedAt = now)

    companion object {
        fun queued(
            taskId: String,
            userPublicId: String,
            originalFileName: String,
            now: LocalDateTime
        ): TranscodeTaskState =
            TranscodeTaskState(
                taskId = taskId,
                userPublicId = userPublicId,
                originalFileName = originalFileName,
                jobId = null,
                status = TranscodeTaskStatus.QUEUED,
                errorMessage = null,
                media = null,
                createdAt = now,
                updatedAt = now
            )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AwsSnsMessage(
    @get:JsonProperty("Type") val type: String?,
    @get:JsonProperty("MessageId") val messageId: String?,
    @get:JsonProperty("TopicArn") val topicArn: String?,
    @get:JsonProperty("Message") val message: String?,
    @get:JsonProperty("Timestamp") val timestamp: String?,
    @get:JsonProperty("SubscribeURL") val subscribeUrl: String?
)