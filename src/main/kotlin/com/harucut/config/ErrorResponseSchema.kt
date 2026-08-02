package com.harucut.config

import com.harucut.util.response.FieldErrorResponse
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Swagger(OpenAPI) 문서 전용 에러 응답 스키마.
 *
 * `@ApiResponse` 에 `content` 를 지정하지 않으면 springdoc이 컨트롤러 메서드의 실제 반환 타입(성공 응답)으로
 * 에러 응답 스키마까지 채워버린다([OpenApiErrorResponseConfig] 참고). 이 타입들은 [OpenApiErrorResponseConfig]
 * 가 4xx/5xx 응답의 문서 스키마를 대체하는 데만 쓰이며, 런타임 직렬화에는 전혀 관여하지 않는다.
 * 실제 에러 응답 형태는 `com.harucut.util.response.Response` 와 `com.harucut.exception.GlobalExceptionHandler` 를 따른다.
 */
@Schema(description = "에러 응답")
data class ErrorResponse(
    // Kotlin 생성자 파라미터에 use-site target 없이 @Schema를 붙이면 PARAMETER 타깃으로만 적용되어
    // swagger-core가 프로퍼티 스키마(description/example)를 읽지 못한다. @field: 로 명시해야 한다.
    @field:Schema(description = "에러 코드", example = "AUTH-010")
    val code: String,
    @field:Schema(description = "HTTP 상태 코드", example = "401")
    val status: Int,
    @field:Schema(description = "에러 메시지", example = "Authentication failed.")
    val message: String
)

/**
 * `@Valid` 검증 실패(400) 전용 에러 응답 스키마.
 * `GlobalExceptionHandler.handleMethodArgumentNotValidException` 이 `data` 에 필드별 검증 실패 목록을 담아 반환한다.
 */
@Schema(description = "검증 실패(400) 응답")
data class ValidationErrorResponse(
    @field:Schema(description = "에러 코드", example = "GEN-003")
    val code: String,
    @field:Schema(description = "HTTP 상태 코드", example = "400")
    val status: Int,
    @field:Schema(description = "에러 메시지", example = "Validation failed.")
    val message: String,
    @field:Schema(description = "필드별 검증 실패 상세")
    val data: List<FieldErrorResponse>
)
