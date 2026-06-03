package com.harucut.common.aop

import com.harucut.common.annotation.PreventDuplicateRequest
import com.harucut.common.enums.LockStrategy
import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class DuplicateRequestAspectTest {

    @MockK
    private lateinit var redisTemplate: StringRedisTemplate

    @MockK
    private lateinit var valueOperations: ValueOperations<String, String>

    @MockK
    private lateinit var joinPoint: ProceedingJoinPoint

    @MockK
    private lateinit var methodSignature: MethodSignature

    @InjectMockKs
    private lateinit var aspect: DuplicateRequestAspect

    private val annotation: PreventDuplicateRequest = mockk()

    @BeforeEach
    fun setUp() {
        every { redisTemplate.opsForValue() } returns valueOperations
        every { joinPoint.signature } returns methodSignature
        every { annotation.strategy } returns LockStrategy.FAIL_CLOSE
    }

    @Test
    @DisplayName("[성공] Redis에 키가 없으면 락을 걸고 메서드를 실행한다")
    fun acquiresLockAndProceeds() {
        // given
        val email = "test@harucut.com"
        val requestDto = TestRequestDto(email)
        val method = TestController::class.java.getMethod("testMethod", TestRequestDto::class.java)

        every { methodSignature.method } returns method
        every { joinPoint.args } returns arrayOf(requestDto)
        every { annotation.key } returns "#request.email"
        every { annotation.time } returns 3000L
        every { annotation.timeUnit } returns TimeUnit.MILLISECONDS

        val expectedKey = "duplicate:request:testMethod:$email"

        every {
            valueOperations.setIfAbsent(expectedKey, "locked", any<Duration>())
        } returns true
        every { joinPoint.proceed() } returns "ok"

        // when
        aspect.checkDuplicate(joinPoint, annotation)

        // then
        verify(exactly = 1) { joinPoint.proceed() }
    }

    @Test
    @DisplayName("Redis에 이미 키가 존재하면 DUPLICATE_REQUEST 예외를 던진다")
    fun throwsOnDuplicateRequest() {
        val email = "test@harucut.com"
        val requestDto = TestRequestDto(email)
        val method = TestController::class.java.getMethod("testMethod", TestRequestDto::class.java)

        every { methodSignature.method } returns method
        every { joinPoint.args } returns arrayOf(requestDto)
        every { annotation.key } returns "#request.email"
        every { annotation.time } returns 3000L
        every { annotation.timeUnit } returns TimeUnit.MILLISECONDS

        every {
            valueOperations.setIfAbsent("duplicate:request:testMethod:$email", "locked", any<Duration>())
        } returns false

        val ex = assertThrows<BusinessException> {
            aspect.checkDuplicate(joinPoint, annotation)
        }
        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.DUPLICATE_REQUEST)
        verify(exactly = 0) { joinPoint.proceed() }
    }

    @Test
    @DisplayName("SpEL 키를 지정하지 않으면 메서드 이름만으로 Redis 키를 생성한다")
    fun usesMethodNameWhenNoSpelKey() {
        val method = TestController::class.java.getMethod("noArgMethod")

        every { methodSignature.method } returns method
        every { annotation.key } returns ""
        every { annotation.time } returns 3000L
        every { annotation.timeUnit } returns TimeUnit.MILLISECONDS
        every {
            valueOperations.setIfAbsent("duplicate:request:noArgMethod", "locked", any<Duration>())
        } returns true
        every { joinPoint.proceed() } returns "ok"

        aspect.checkDuplicate(joinPoint, annotation)

        verify(exactly = 1) {
            valueOperations.setIfAbsent("duplicate:request:noArgMethod", "locked", any<Duration>())
        }
        verify(exactly = 1) { joinPoint.proceed() }
    }

    @Test
    @DisplayName("Redis 오류 발생 시 FAIL_CLOSE 전략이면 REDIS_CONNECTION_ERROR 예외를 던진다")
    fun throwsOnRedisFailureWithFailClose() {
        val method = TestController::class.java.getMethod("noArgMethod")

        every { methodSignature.method } returns method
        every { annotation.key } returns ""
        every { annotation.time } returns 3000L
        every { annotation.timeUnit } returns TimeUnit.MILLISECONDS
        every { annotation.strategy } returns LockStrategy.FAIL_CLOSE
        every { valueOperations.setIfAbsent(any(), any(), any<Duration>()) } throws RuntimeException("Connection refused")

        val ex = assertThrows<BusinessException> {
            aspect.checkDuplicate(joinPoint, annotation)
        }
        assertThat(ex.errorCode).isEqualTo(GlobalErrorCode.REDIS_CONNECTION_ERROR)
    }

    @Test
    @DisplayName("Redis 오류 발생 시 FAIL_OPEN 전략이면 예외 없이 메서드를 실행한다")
    fun proceedsOnRedisFailureWithFailOpen() {
        val method = TestController::class.java.getMethod("noArgMethod")

        every { methodSignature.method } returns method
        every { annotation.key } returns ""
        every { annotation.time } returns 3000L
        every { annotation.timeUnit } returns TimeUnit.MILLISECONDS
        every { annotation.strategy } returns LockStrategy.FAIL_OPEN
        every { valueOperations.setIfAbsent(any(), any(), any<Duration>()) } throws RuntimeException("Connection refused")
        every { joinPoint.proceed() } returns "ok"

        aspect.checkDuplicate(joinPoint, annotation)

        verify(exactly = 1) { joinPoint.proceed() }
    }

    data class TestRequestDto(val email: String)

    class TestController {
        fun testMethod(request: TestRequestDto) {}
        fun noArgMethod() {}
    }
}