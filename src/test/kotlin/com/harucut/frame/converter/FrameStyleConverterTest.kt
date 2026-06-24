package com.harucut.frame.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FrameStyleConverterTest {

    private val converter = FrameStyleConverter(jacksonObjectMapper())

    @Nested
    inner class ConvertToJson {

        @Test
        @DisplayName("스타일 맵을 JSON 문자열로 직렬화한다")
        fun serialize() {
            val json = converter.convertToJson(mapOf("color" to "red", "fontSize" to 14))

            assertThat(json).contains("\"color\":\"red\"")
            assertThat(json).contains("\"fontSize\":14")
        }

        @Test
        @DisplayName("null이거나 비어있으면 빈 객체 '{}'를 반환한다")
        fun emptyOrNull() {
            assertThat(converter.convertToJson(null)).isEqualTo("{}")
            assertThat(converter.convertToJson(emptyMap())).isEqualTo("{}")
        }
    }

    @Nested
    inner class ConvertToMap {

        @Test
        @DisplayName("JSON 문자열을 스타일 맵으로 역직렬화한다")
        fun deserialize() {
            val map = converter.convertToMap("""{"color":"red","fontSize":14}""")

            assertThat(map).containsEntry("color", "red")
            assertThat(map).containsEntry("fontSize", 14)
        }

        @Test
        @DisplayName("null이거나 비어있으면 빈 맵을 반환한다")
        fun emptyOrNull() {
            assertThat(converter.convertToMap(null)).isEmpty()
            assertThat(converter.convertToMap("")).isEmpty()
        }

        @Test
        @DisplayName("잘못된 JSON이면 빈 맵을 반환한다(예외 삼킴)")
        fun invalidJson() {
            assertThat(converter.convertToMap("{not-json")).isEmpty()
        }
    }
}
