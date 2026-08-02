package com.harucut.util.response

import io.swagger.v3.oas.annotations.media.Schema

// 클래스 레벨 @Schema를 명시하지 않으면, 이 타입을 참조하는 프로퍼티(ValidationErrorResponse.data)의
// description이 컴포넌트 자체의 description으로 새어 나간다(swagger-core가 참조 프로퍼티의 description을
// 대상 컴포넌트에 병합함). 명시적으로 지정해 오염을 막는다.
@Schema(description = "필드 검증 실패 상세")
data class FieldErrorResponse(
    // @field: 명시 필요 이유는 com.harucut.config.ErrorResponse 참고(use-site target 없으면 PARAMETER로만
    // 적용되어 swagger-core가 description/example을 읽지 못한다). Jackson은 @Schema를 인식하지 않으므로
    // 직렬화 결과에는 영향이 없다.
    @field:Schema(description = "검증 실패한 필드명", example = "email")
    val field: String,
    @field:Schema(description = "검증 실패 메시지", example = "must not be blank")
    val message: String?,
    @field:Schema(description = "거부된 값", example = "invalid-email")
    val rejectedValue: Any?
)