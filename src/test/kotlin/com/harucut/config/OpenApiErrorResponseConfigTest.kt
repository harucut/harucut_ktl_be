package com.harucut.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import jakarta.validation.Valid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod

@DisplayName("에러 응답 스키마 OperationCustomizer")
class OpenApiErrorResponseConfigTest {

    class DummyRequest
    class DummyController {
        fun withValidBody(@Valid body: DummyRequest) {}
        fun withoutValidBody(id: Long) {}
    }

    private val config = OpenApiErrorResponseConfig()
    private val openAPI = OpenAPI().components(Components())
    private val customizer = config.errorResponseOperationCustomizer(openAPI)

    private fun handlerMethod(methodName: String, vararg paramTypes: Class<*>): HandlerMethod =
        HandlerMethod(DummyController(), DummyController::class.java.getDeclaredMethod(methodName, *paramTypes))

    private fun operationWith(vararg responses: Pair<String, ApiResponse>): Operation {
        val apiResponses = ApiResponses()
        responses.forEach { (code, response) -> apiResponses.addApiResponse(code, response) }
        return Operation().responses(apiResponses)
    }

    private fun schemaRefOf(operation: Operation, statusCode: String): String? =
        operation.responses[statusCode]?.content?.get("application/json")?.schema?.`$ref`

    @Nested
    @DisplayName("2xx 응답")
    inner class SuccessResponses {

        @Test
        @DisplayName("200 응답의 content 는 그대로 유지된다")
        fun keepsSuccessSchema() {
            val successContent = Content().addMediaType(
                "application/json",
                MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ResponseListFrameResponse"))
            )
            val operation = operationWith("200" to ApiResponse().description("조회 성공").content(successContent))

            customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(operation.responses["200"]!!.content).isSameAs(successContent)
        }
    }

    @Nested
    @DisplayName("4xx/5xx 응답")
    inner class ErrorResponses {

        @Test
        @DisplayName("@Valid 가 없으면 ErrorResponse 스키마로 대체된다")
        fun replacesWithErrorResponse() {
            val operation = operationWith(
                "401" to ApiResponse().description("인증 필요"),
                "403" to ApiResponse().description("관리자 권한 없음")
            )

            customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(schemaRefOf(operation, "401")).isEqualTo("#/components/schemas/ErrorResponse")
            assertThat(schemaRefOf(operation, "403")).isEqualTo("#/components/schemas/ErrorResponse")
            assertThat(operation.responses["401"]!!.description).isEqualTo("인증 필요")
            assertThat(operation.responses["403"]!!.description).isEqualTo("관리자 권한 없음")
        }

        @Test
        @DisplayName("@Valid 파라미터가 있는 400 은 ValidationErrorResponse 스키마로 대체된다")
        fun replacesValidationFailureWithValidationErrorResponse() {
            val operation = operationWith("400" to ApiResponse().description("검증 실패"))

            customizer.customize(operation, handlerMethod("withValidBody", DummyRequest::class.java))

            assertThat(schemaRefOf(operation, "400")).isEqualTo("#/components/schemas/ValidationErrorResponse")
        }

        @Test
        @DisplayName("@Valid 파라미터가 없는 400 은 ErrorResponse 스키마로 대체된다")
        fun replacesNonValidationFailureWithErrorResponse() {
            val operation = operationWith("400" to ApiResponse().description("잘못된 요청"))

            customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(schemaRefOf(operation, "400")).isEqualTo("#/components/schemas/ErrorResponse")
        }

        @Test
        @DisplayName("content 가 아예 없던 응답에도 에러 스키마가 채워진다")
        fun fillsMissingContent() {
            val operation = operationWith("404" to ApiResponse().description("찾을 수 없음"))
            assertThat(operation.responses["404"]!!.content).isNull()

            customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(schemaRefOf(operation, "404")).isEqualTo("#/components/schemas/ErrorResponse")
        }

        @Test
        @DisplayName("5xx 응답도 ErrorResponse 스키마로 대체된다")
        fun replaces5xxWithErrorResponse() {
            val operation = operationWith("500" to ApiResponse().description("서버 내부 오류"))

            customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(schemaRefOf(operation, "500")).isEqualTo("#/components/schemas/ErrorResponse")
            assertThat(operation.responses["500"]!!.description).isEqualTo("서버 내부 오류")
        }
    }

    @Nested
    @DisplayName("경계 케이스")
    inner class EdgeCases {

        @Test
        @DisplayName("responses 가 null 이면 예외 없이 그대로 반환한다")
        fun handlesNullResponses() {
            val operation = Operation()
            assertThat(operation.responses).isNull()

            val result = customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(result.responses).isNull()
        }

        @Test
        @DisplayName("상태코드가 숫자가 아닌 default 응답은 건드리지 않는다")
        fun keepsNonNumericStatusCodeUntouched() {
            val defaultContent = Content().addMediaType(
                "application/json",
                MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ResponseListFrameResponse"))
            )
            val operation = operationWith("default" to ApiResponse().description("기타 오류").content(defaultContent))

            customizer.customize(operation, handlerMethod("withoutValidBody", Long::class.java))

            assertThat(operation.responses["default"]!!.content).isSameAs(defaultContent)
        }
    }
}
