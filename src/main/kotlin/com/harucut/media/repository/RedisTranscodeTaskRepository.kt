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

    companion object {
        private const val TASK_KEY_PREFIX = "media:transcode:task:"
        private const val JOB_KEY_PREFIX  = "media:transcode:job:"
        private val TTL = Duration.ofDays(7)
    }

    override fun save(state: TranscodeTaskState) {
        val key = TASK_KEY_PREFIX + state.taskId
        val payload = runCatching { objectMapper.writeValueAsString(state) }
            .getOrElse { throw IllegalStateException("TranscodeTaskState 직렬화 실패", it) }
        redisTemplate.opsForValue().set(key, payload, TTL)
    }

    override fun findByTaskId(taskId: String): TranscodeTaskState? {
        val key = TASK_KEY_PREFIX + taskId
        val payload = redisTemplate.opsForValue().get(key) ?: return null
        return runCatching { objectMapper.readValue(payload, TranscodeTaskState::class.java) }
            .onFailure { log.error("TranscodeTaskState 역직렬화 실패. key={}", key, it) }
            .getOrNull()
    }

    override fun linkJobToTask(jobId: String, taskId: String) {
        redisTemplate.opsForValue().set(JOB_KEY_PREFIX + jobId, taskId, TTL)
    }

    override fun findTaskIdByJobId(jobId: String): String? =
        redisTemplate.opsForValue().get(JOB_KEY_PREFIX + jobId)
}
