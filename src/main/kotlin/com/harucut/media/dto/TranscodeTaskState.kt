package com.harucut.media.dto

import com.harucut.media.dto.response.UserMediaResponse
import com.harucut.media.enums.TranscodeTaskStatus
import java.time.LocalDateTime

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
    companion object {
        fun queued(
            taskId: String,
            userPublicId: String,
            originalFileName: String,
            now: LocalDateTime
        ) = TranscodeTaskState(
            taskId           = taskId,
            userPublicId     = userPublicId,
            originalFileName = originalFileName,
            jobId            = null,
            status           = TranscodeTaskStatus.QUEUED,
            errorMessage     = null,
            media            = null,
            createdAt        = now,
            updatedAt        = now
        )
    }

    fun withSubmitted(newJobId: String, now: LocalDateTime) = copy(
        jobId        = newJobId,
        status       = TranscodeTaskStatus.SUBMITTED,
        errorMessage = null,
        updatedAt    = now
    )

    fun withProgressing(now: LocalDateTime) = copy(
        status    = TranscodeTaskStatus.PROGRESSING,
        updatedAt = now
    )

    fun withComplete(media: UserMediaResponse, now: LocalDateTime) = copy(
        status       = TranscodeTaskStatus.COMPLETE,
        errorMessage = null,
        media        = media,
        updatedAt    = now
    )

    fun withError(message: String, now: LocalDateTime) = copy(
        status       = TranscodeTaskStatus.ERROR,
        errorMessage = message,
        updatedAt    = now
    )
}
