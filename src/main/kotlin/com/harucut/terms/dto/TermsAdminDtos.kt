package com.harucut.terms.dto

import com.harucut.terms.entity.Terms
import com.harucut.terms.entity.TermsVersion
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "약관 생성 요청")
data class CreateTermsRequest(
    @field:NotBlank(message = "약관 코드는 필수입니다.")
    @field:Pattern(regexp = "^[a-z0-9-]{1,50}$", message = "약관 코드는 소문자·숫자·하이픈만 사용할 수 있습니다.")
    @Schema(description = "약관 코드 (불변 slug)", example = "tos")
    val code: String,

    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    @Schema(description = "약관 제목", example = "이용약관")
    val title: String,

    @field:NotNull(message = "필수 동의 여부는 필수입니다.")
    @Schema(description = "필수 동의 여부", example = "true")
    val required: Boolean,

    @field:NotBlank(message = "본문은 필수입니다.")
    @Schema(description = "약관 본문", example = "제1조 (목적) ...")
    val content: String
)

@Schema(description = "약관 개정 요청")
data class ReviseTermsRequest(
    @field:NotBlank(message = "본문은 필수입니다.")
    @Schema(description = "개정된 약관 본문", example = "제1조 (목적, 개정) ...")
    val content: String
)

@Schema(description = "관리자 약관 목록 응답")
data class TermsAdminResponse(
    @Schema(description = "약관 ID", example = "1")
    val termsId: Long,
    @Schema(description = "약관 코드", example = "tos")
    val code: String,
    @Schema(description = "약관 제목", example = "이용약관")
    val title: String,
    @Schema(description = "필수 동의 여부", example = "true")
    val required: Boolean,
    @Schema(description = "활성 여부", example = "true")
    val active: Boolean,
    @Schema(description = "현재 최신 버전", example = "1")
    val latestVersion: Int,
    @Schema(description = "현재 본문", example = "제1조 (목적) ...")
    val content: String
) {
    companion object {
        fun from(terms: Terms, latest: TermsVersion) = TermsAdminResponse(
            termsId = terms.id!!,
            code = terms.code,
            title = terms.title,
            required = terms.required,
            active = terms.active,
            latestVersion = latest.version,
            content = latest.content
        )
    }
}
