package com.harucut.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

// [회귀] RedisConfig가 인자 없는 LettuceConnectionFactory()를 직접 빈으로 등록하면
// RedisAutoConfiguration이 @ConditionalOnMissingBean으로 물러나 spring.data.redis.*
// 프로퍼티가 통째로 무시되고 항상 localhost:6379로 접속하던 버그(이메일 인증 500 원인).
@DisplayName("RedisConfig")
class RedisConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration::class.java))
        .withUserConfiguration(RedisConfig::class.java)

    @Test
    @DisplayName("[회귀] spring.data.redis.host/port 프로퍼티가 실제 연결 팩토리에 반영된다")
    fun usesConfiguredRedisHostAndPort() {
        contextRunner
            .withPropertyValues("spring.data.redis.host=redis", "spring.data.redis.port=16379")
            .run { context ->
                val connectionFactory = context.getBean(LettuceConnectionFactory::class.java)

                assertThat(connectionFactory.hostName).isEqualTo("redis")
                assertThat(connectionFactory.port).isEqualTo(16379)
            }
    }
}
