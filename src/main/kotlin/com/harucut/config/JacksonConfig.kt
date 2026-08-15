package com.harucut.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

// 서버는 항상 UTC로 뜨는 것을 전제로 LocalDateTime(createdAt/updatedAt 등)을 저장한다(Dockerfile ENV TZ=UTC 참고).
// 오프셋 없이 그대로 내려주면 클라이언트가 자기 로컬 타임존으로 오해석해 시간이 밀리므로,
// 직렬화 시에는 "이 값은 UTC다"를 명시하기 위해 Instant로 변환해 'Z' 접미사를 붙인다.
class UtcLocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {
    override fun serialize(value: LocalDateTime, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toInstant(ZoneOffset.UTC).toString())
    }
}

class UtcLocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        val value = p.valueAsString
            ?: return ctxt.reportInputMismatch(LocalDateTime::class.java, "LocalDateTime 값이 없습니다.")
        return try {
            Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            // 오프셋 없는 레거시 포맷(예: "2026-12-31T23:59:59")은 UTC wall-clock으로 그대로 해석한다.
            LocalDateTime.parse(value)
        }
    }
}

@Configuration
class JacksonConfig {

    @Bean
    fun utcLocalDateTimeJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.serializerByType(LocalDateTime::class.java, UtcLocalDateTimeSerializer())
            builder.deserializerByType(LocalDateTime::class.java, UtcLocalDateTimeDeserializer())
        }
}
