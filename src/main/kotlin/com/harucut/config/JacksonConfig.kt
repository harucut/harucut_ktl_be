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

// 서버 내부(DB/JVM/배치 스케줄)는 항상 UTC로 LocalDateTime(createdAt/updatedAt 등)을 다룬다
// (컨테이너는 UTC로 고정 — Dockerfile ENV TZ=UTC 참고. 구독 갱신/만료 등 배치 트리거 시각에 영향 없음).
// API 응답에서는 프론트가 그대로 표시할 수 있도록 KST(+09:00)로 변환해 오프셋과 함께 내려준다.
val KST: ZoneOffset = ZoneOffset.of("+09:00")

class KstLocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {
    override fun serialize(value: LocalDateTime, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.atOffset(ZoneOffset.UTC).withOffsetSameInstant(KST).toString())
    }
}

// 오프셋이 있는 입력(Z, +09:00 등)은 그 오프셋 기준으로 파싱해 내부 저장 기준인 UTC wall-clock으로 정규화하고,
// 오프셋 없는 레거시 입력은 기존 동작대로 UTC wall-clock으로 그대로 해석한다.
class KstLocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        val value = p.valueAsString
            ?: return ctxt.reportInputMismatch(LocalDateTime::class.java, "LocalDateTime 값이 없습니다.")
        return try {
            Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            LocalDateTime.parse(value)
        }
    }
}

@Configuration
class JacksonConfig {

    @Bean
    fun kstLocalDateTimeJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.serializerByType(LocalDateTime::class.java, KstLocalDateTimeSerializer())
            builder.deserializerByType(LocalDateTime::class.java, KstLocalDateTimeDeserializer())
        }
}
