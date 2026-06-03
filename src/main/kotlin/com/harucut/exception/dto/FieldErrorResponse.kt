package com.harucut.exception.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "필드 단위 검증 에러 응답")
data class FieldErrorResponse(
    @Schema(description = "에러가 발생한 필드명", example = "email")
    val field: String,
    @Schema(description = "에러 메시지", example = "유효한 이메일 형식이 아닙니다.")
    val message: String?,
    @Schema(description = "요청에 전달된 잘못된 값")
    val rejectedValue: Any?,
)