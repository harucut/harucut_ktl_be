package com.harucut.config

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import jakarta.validation.Valid
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

/**
 * springdoc은 `@ApiResponse` 에 `content` 를 지정하지 않으면 해당 응답 코드의 스키마를 컨트롤러 메서드의
 * 실제 반환 타입(즉 성공 응답 타입)으로 채운다. 이 프로젝트의 모든 컨트롤러는 성공/실패를 공용 래퍼
 * `Response<T>` 로 반환하기 때문에, 컨트롤러 152곳 전부에서 401/403/404 등 에러 응답 문서가 성공 응답과
 * 동일한 스키마(`ResponseXxx`)를 그대로 가리키는 문제가 있었다.
 *
 * 컨트롤러를 일일이 고치는 대신, 여기서 모든 operation의 4xx/5xx 응답 스키마를 실제 런타임 에러 응답 형태와
 * 일치하는 문서 전용 스키마([ErrorResponse], [ValidationErrorResponse])로 일괄 대체한다.
 * 2xx 응답은 건드리지 않는다.
 */
@Configuration
class OpenApiErrorResponseConfig {

    @Bean
    fun errorResponseOperationCustomizer(openAPI: OpenAPI): OperationCustomizer {
        val errorResponseRef = registerSchema(openAPI, ErrorResponse::class.java)
        val validationErrorResponseRef = registerSchema(openAPI, ValidationErrorResponse::class.java)

        return OperationCustomizer { operation, handlerMethod ->
            val hasValidParam = handlerMethod.methodParameters.any { parameter ->
                parameter.hasParameterAnnotation(Valid::class.java) ||
                    parameter.hasParameterAnnotation(Validated::class.java)
            }

            operation.responses?.forEach { (statusCode, apiResponse) ->
                val status = statusCode.toIntOrNull() ?: return@forEach
                if (status < 400) return@forEach

                val schemaRef = if (status == 400 && hasValidParam) validationErrorResponseRef else errorResponseRef
                // 기존 description 은 그대로 두고 content(스키마)만 교체한다.
                apiResponse.content = Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`(schemaRef))
                )
            }
            operation
        }
    }

    /**
     * [type] 을 OpenAPI 스키마로 변환해 전역 `components.schemas` 에 등록하고, `$ref` 경로를 반환한다.
     *
     * **불변식: 반드시 빈 생성 시점(= 이 함수를 호출하는 `errorResponseOperationCustomizer` 의 람다 바깥)에서
     * 호출해야 한다.** springdoc의 `OpenAPIService.build()` 는 요청마다 등록된 `OpenAPI` 빈을 JSON으로
     * 딥카피(clone)해서 응답을 만든다. 이 등록을 커스터마이저 람다 안(= 클론 이후 시점)으로 옮기면,
     * operation의 `$ref` 는 여전히 `#/components/schemas/ErrorResponse` 를 가리키지만 클론된 문서의
     * `components.schemas` 에는 정작 `ErrorResponse` 가 없어 dangling reference가 되고, Swagger UI가
     * 조용히 깨진다(`remove-broken-reference-definitions` 로도 걸러지지 않는다). `by lazy` 등으로 지연
     * 초기화하는 리팩터링은 이 이유로 하면 안 된다.
     */
    private fun registerSchema(openAPI: OpenAPI, type: Class<*>): String {
        val resolved = ModelConverters.getInstance()
            .resolveAsResolvedSchema(AnnotatedType(type).resolveAsRef(true))
        resolved.referencedSchemas.forEach { (name, schema) ->
            openAPI.components.addSchemas(name, schema)
        }
        return resolved.schema.`$ref`
    }
}
