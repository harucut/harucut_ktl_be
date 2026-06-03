package com.harucut.common.aop

import com.harucut.common.annotation.PreventDuplicateRequest
import com.harucut.common.enums.LockStrategy
import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.StandardReflectionParameterNameDiscoverer
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.time.Duration

@Aspect
@Component
class DuplicateRequestAspect(
    private val redisTemplate: StringRedisTemplate,
) {

    private val parser = SpelExpressionParser()
    private val discoverer = StandardReflectionParameterNameDiscoverer()

    @Around("@annotation(preventDuplicateRequest)")
    fun checkDuplicate(
        joinPoint: ProceedingJoinPoint,
        preventDuplicateRequest: PreventDuplicateRequest
    ): Any? {
        val uniqueKey = generateUniqueKey(joinPoint, preventDuplicateRequest.key)
        val redisKey = "duplicate:request:$uniqueKey"

        val time = preventDuplicateRequest.time
        val unit = preventDuplicateRequest.timeUnit
        val strategy = preventDuplicateRequest.strategy

        try {
            val isSuccess = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "locked", Duration.ofMillis(unit.toMillis(time)))

            if (isSuccess == null || !isSuccess) {
                log.warn("중복 요청 차단됨 - Key: {}", redisKey)
                throw BusinessException(GlobalErrorCode.DUPLICATE_REQUEST)
            }
        } catch (e: Exception) {
            if (e is BusinessException) throw e

            if (strategy == LockStrategy.FAIL_OPEN) {
                log.error("Redis 연결 실패 - Fail Open 정책에 의해 로직 실행 허용. Error: {}", e.message)
            } else {
                log.error("Redis 연결 실패 - Fail Close 정책에 의해 로직 차단. Error: {}", e.message)
                throw BusinessException(GlobalErrorCode.REDIS_CONNECTION_ERROR)
            }
        }

        return joinPoint.proceed()
    }

    private fun generateUniqueKey(joinPoint: ProceedingJoinPoint, spelKey: String): String {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method

        // SpEL 키 미지정 시 메서드명만 사용
        if (!StringUtils.hasText(spelKey)) {
            return method.name
        }

        // SpEL 평가
        val context = StandardEvaluationContext()
        val parameterNames = discoverer.getParameterNames(method)
        val args = joinPoint.args

        parameterNames?.forEachIndexed { i, name ->
            context.setVariable(name, args[i])
        }

        val value = parser.parseExpression(spelKey).getValue(context)
        return "${method.name}:${value?.toString() ?: "unknown"}"
    }

    companion object {
        private val log = LoggerFactory.getLogger(DuplicateRequestAspect::class.java)
    }
}