package com.harucut.frame.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.frame.enums.ComponentType
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

// zIndex 직렬화/역직렬화 필드명을 고정한다.
// Kotlin data class의 getZIndex() getter는 Jackson bean 명명 규칙상 "zindex"(소문자)로 직렬화되는데,
// 이게 조용히 바뀌면 프론트(zIndex를 기대)가 깨진다. @get:JsonProperty("zIndex")로 응답 필드명을 고정했고,
// 요청은 과거 Swagger에서 소문자 zindex로 노출됐던 이력 때문에 프론트가 아직 zindex도 보내고 있어 @JsonAlias로 함께 받는다.
@JsonTest
class FrameDtosSerializationTest {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Nested
    inner class ComponentResponseSerialization {

        @Test
        @DisplayName("zIndex는 zIndex(카멜케이스)로 직렬화된다")
        fun serializesZIndexAsCamelCase() {
            val response = FrameResponse.ComponentResponse(
                id = 1L, type = ComponentType.PHOTO, source = "https://x", key = "k",
                x = 0.0, y = 0.0, width = 1.0, height = 1.0, scale = 1.0,
                rotation = 0.0, zIndex = 3, style = emptyMap()
            )

            val json = objectMapper.readTree(objectMapper.writeValueAsString(response))

            assertThat(json.has("zIndex")).isTrue()
            assertThat(json.get("zIndex").asInt()).isEqualTo(3)
            assertThat(json.has("zindex")).isFalse()
        }
    }

    @Nested
    inner class ComponentRequestDeserialization {

        @Test
        @DisplayName("zIndex(카멜케이스)로 온 요청을 파싱한다")
        fun parsesCamelCaseZIndex() {
            val json = """{"type":"PHOTO","source":"s","zIndex":5}"""

            val request = objectMapper.readValue(json, FrameCreateRequest.ComponentRequest::class.java)

            assertThat(request.zIndex).isEqualTo(5)
        }

        @Test
        @DisplayName("[하위호환] zindex(소문자)로 온 요청도 파싱한다")
        fun parsesLowercaseZindexForBackwardCompatibility() {
            val json = """{"type":"PHOTO","source":"s","zindex":7}"""

            val request = objectMapper.readValue(json, FrameCreateRequest.ComponentRequest::class.java)

            assertThat(request.zIndex).isEqualTo(7)
        }

        // 프론트가 과도기적으로 zIndex/zindex 둘 다 보내고 있다. 예외 없이 JSON에 나중에 나온 값이 승리하는 것이
        // 실측 동작이며, 동작을 바꾸는 게 아니라 현재 동작을 고정해두기 위한 테스트다.
        @Test
        @DisplayName("[현행동작 고정] zIndex와 zindex가 함께 오면 JSON에 나중에 나온 값이 승리한다")
        fun laterFieldWinsWhenBothPresent() {
            val zIndexLast = """{"type":"PHOTO","source":"s","zIndex":5,"zindex":7}"""
            val zindexLast = """{"type":"PHOTO","source":"s","zindex":7,"zIndex":5}"""

            val requestZIndexLast = objectMapper.readValue(zIndexLast, FrameCreateRequest.ComponentRequest::class.java)
            val requestZindexLast = objectMapper.readValue(zindexLast, FrameCreateRequest.ComponentRequest::class.java)

            assertThat(requestZIndexLast.zIndex).isEqualTo(7)
            assertThat(requestZindexLast.zIndex).isEqualTo(5)
        }
    }

    @Nested
    inner class ComponentRequestSwaggerSchema {

        @Test
        @DisplayName("Swagger 요청 스키마의 속성명도 zIndex(카멜케이스)이다")
        fun exposesZIndexPropertyName() {
            val resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(AnnotatedType(FrameCreateRequest.ComponentRequest::class.java))

            assertThat(resolved.schema.properties.keys).contains("zIndex")
            assertThat(resolved.schema.properties.keys).doesNotContain("zindex")
        }
    }
}
