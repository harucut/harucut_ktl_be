package com.harucut.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

// 직렬화기/역직렬화기 자체 로직을 Spring 컨텍스트 없이 검증하는 순수 단위 테스트.
// 실제 앱 ObjectMapper 배선(Jackson2ObjectMapperBuilderCustomizer 적용 여부)은
// JacksonConfigIntegrationTest 에서 확인한다.
@DisplayName("KST LocalDateTime (역)직렬화기")
class JacksonConfigTest {

    private val objectMapper: ObjectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(
            SimpleModule()
                .addSerializer(LocalDateTime::class.java, KstLocalDateTimeSerializer())
                .addDeserializer(LocalDateTime::class.java, KstLocalDateTimeDeserializer())
        )

    @Nested
    @DisplayName("직렬화")
    inner class Serialization {

        @Test
        @DisplayName("내부 UTC LocalDateTime을 KST(+09:00)로 변환해 오프셋과 함께 직렬화한다")
        fun serializesWithKstOffset() {
            val value = LocalDateTime.of(2026, 8, 14, 10, 54, 45)

            val json = objectMapper.writeValueAsString(value)

            assertThat(json).isEqualTo("\"2026-08-14T19:54:45+09:00\"")
        }
    }

    @Nested
    @DisplayName("역직렬화")
    inner class Deserialization {

        @Test
        @DisplayName("직렬화한 KST 문자열을 원래 UTC LocalDateTime으로 역직렬화한다(라운드트립)")
        fun deserializesRoundTrip() {
            val original = LocalDateTime.of(2026, 8, 14, 19, 54, 45, 920_636_000)

            val json = objectMapper.writeValueAsString(original)
            val restored = objectMapper.readValue(json, LocalDateTime::class.java)

            assertThat(restored).isEqualTo(original)
        }

        @Test
        @DisplayName("+09:00 오프셋 문자열은 UTC wall-clock으로 정규화해 역직렬화한다")
        fun deserializesKstOffsetToUtc() {
            val restored = objectMapper.readValue("\"2026-08-15T04:54:45+09:00\"", LocalDateTime::class.java)

            assertThat(restored).isEqualTo(LocalDateTime.of(2026, 8, 14, 19, 54, 45))
        }

        @Test
        @DisplayName("오프셋 없는 레거시 문자열도 UTC wall-clock으로 그대로 역직렬화한다")
        fun deserializesOffsetLessLegacyFormat() {
            val restored = objectMapper.readValue("\"2026-12-31T23:59:59\"", LocalDateTime::class.java)

            assertThat(restored).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59, 59))
        }
    }
}
