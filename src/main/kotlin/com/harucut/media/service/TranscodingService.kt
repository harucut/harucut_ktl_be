package com.harucut.media.service

import com.harucut.media.dto.TranscodeTaskStatusResponse
import com.harucut.media.dto.TranscodeTaskSubmitResponse

interface TranscodingService {

    /** WebM 업로드 완료 후 MediaConvert 변환 작업 제출 */
    fun submitTranscodeTask(userPublicId: String, fileName: String): TranscodeTaskSubmitResponse

    /** taskId 기준 변환 상태 조회 (소유권 검증 포함) */
    fun getTaskStatus(taskId: String, userPublicId: String): TranscodeTaskStatusResponse

    /** 웹훅: 진행 중 상태 반영 */
    fun markProgressing(jobId: String)

    /** 웹훅: 변환 완료 — 변환본/썸네일 영속화 후 상태 반영 */
    fun handleCompletedJob(
        jobId: String,
        userPublicId: String?,
        originalFileName: String?,
        outputS3Path: String,
        thumbnailS3Path: String?
    )

    /** 웹훅: 변환 실패 상태 반영 */
    fun handleFailedJob(jobId: String, errorMessage: String)
}