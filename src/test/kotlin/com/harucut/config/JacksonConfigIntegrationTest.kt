package com.harucut.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

// JacksonConfigTest는 직렬화기/역직렬화기 로직만 순수 단위로 검증한다.
// 이 테스트는 JacksonConfig의 Jackson2ObjectMapperBuilderCustomizer가 실제 Spring이
// 자동구성한 ObjectMapper에 배선되는지까지 확인한다(순수 단위 테스트로는 잡히지 않는 회귀 방지).
@JsonTest
@Import(JacksonConfig::class)
@DisplayName("JacksonConfig - 실제 앱 ObjectMapper 배선 검증")
class JacksonConfigIntegrationTest {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("직렬화")
    inner class Serialization {

        @Test
        @DisplayName("LocalDateTime을 직렬화하면 Z 접미사가 붙는다")
        fun serializesWithUtcSuffix() {
            val value = LocalDateTime.of(2026, 8, 14, 10, 54, 45)

            val json = objectMapper.writeValueAsString(value)

            assertThat(json).isEqualTo("\"2026-08-14T10:54:45Z\"")
        }
    }

    @Nested
    @DisplayName("역직렬화")
    inner class Deserialization {

        @Test
        @DisplayName("[회귀] 오프셋 없는 문자열을 LocalDateTime으로 역직렬화하면 예외 없이 성공한다")
        fun deserializesOffsetLessStringWithoutException() {
            val restored = objectMapper.readValue("\"2026-12-31T23:59:59\"", LocalDateTime::class.java)

            assertThat(restored).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59, 59))
        }
    }
}
