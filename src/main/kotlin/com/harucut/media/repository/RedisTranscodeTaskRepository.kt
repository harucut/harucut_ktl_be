package com.harucut.media.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.media.dto.TranscodeTaskState
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository


class RedisTranscodeTaskRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : TranscodeTaskRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    // 작업 상태를 JSON으로 직렬화해 저장 (TTL 7일)
    override fun save(state: TranscodeTaskState) {
        val key = TASK_KEY_PREFIX + state.taskId
        val payload = objectMapper.writeValueAsString(state)
        redisTemplate.opsForValue().set(key, payload, TASK_TTL)
    }

    // taskId로 작업 상태 조회 (역직렬화 실패 시 null)
    override fun findByTaskId(taskId: String): TranscodeTaskState? {
        val payload = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId) ?: return null
        return try {
            objectMapper.readValue(payload, TranscodeTaskState::class.java)
        } catch (e: Exception) {
            log.error("변환 작업 상태 역직렬화 실패. taskId={}", taskId, e)
            null
        }
    }

    // jobId → taskId 매핑 저장 (웹훅 역참조용)
    override fun linkJobToTask(jobId: String, taskId: String) {
        redisTemplate.opsForValue().set(JOB_KEY_PREFIX + jobId, taskId, TASK_TTL)
    }

    // jobId로 taskId 역참조
    override fun findTaskIdByJobId(jobId: String): String? =
        redisTemplate.opsForValue().get(JOB_KEY_PREFIX + jobId)

    companion object {
        private const val TASK_KEY_PREFIX = "media:transcode:task:"
        private const val JOB_KEY_PREFIX = "media:transcode:job:"
        private val TASK_TTL: Duration = Duration.ofDays(7)
    }
}