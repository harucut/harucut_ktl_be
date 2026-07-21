package com.harucut.terms.dto

import com.harucut.terms.entity.TermsVersion
import com.harucut.terms.enums.TermsConsentStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "활성 약관 응답")
data class TermsResponse(
    @Schema(description = "약관 코드", example = "tos")
    val code: String,
    @Schema(description = "약관 제목", example = "이용약관")
    val title: String,
    @Schema(description = "필수 동의 여부", example = "true")
    val required: Boolean,
    @Schema(description = "현재 버전", example = "1")
    val version: Int,
    @Schema(description = "약관 본문", example = "제1조 (목적) ...")
    val content: String
) {
    companion object {
        fun from(termsVersion: TermsVersion) = TermsResponse(
            code = termsVersion.terms.code,
            title = termsVersion.terms.title,
            required = termsVersion.terms.required,
            version = termsVersion.version,
            content = termsVersion.content
        )
    }
}

@Schema(description = "내 약관 동의 상태 응답")
data class TermsConsentStatusResponse(
    @Schema(description = "약관 코드", example = "tos")
    val code: String,
    @Schema(description = "약관 제목", example = "이용약관")
    val title: String,
    @Schema(description = "필수 동의 여부", example = "true")
    val required: Boolean,
    @Schema(description = "동의 상태", example = "AGREED")
    val status: TermsConsentStatus,
    @Schema(description = "동의한 버전 (동의한 적 없거나 철회 상태면 null)", example = "1")
    val agreedVersion: Int?,
    @Schema(description = "현재 최신 버전", example = "1")
    val latestVersion: Int
)

@Schema(description = "약관 동의 항목")
data class ConsentItem(
    @field:NotBlank(message = "약관 코드는 필수입니다.")
    @Schema(description = "약관 코드", example = "tos")
    val code: String,
    @Schema(description = "동의 여부 (false = 철회)", example = "true")
    val agreed: Boolean
)
