package com.harucut.media.repository

import com.harucut.media.dto.TranscodeTaskState

interface TranscodeTaskRepository {
    fun save(state: TranscodeTaskState)
    fun findByTaskId(taskId: String): TranscodeTaskState?
    fun linkJobToTask(jobId: String, taskId: String)
    fun findTaskIdByJobId(jobId: String): String?
}
