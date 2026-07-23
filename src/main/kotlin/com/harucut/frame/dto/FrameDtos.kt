package com.harucut.frame.dto

import com.harucut.frame.attributes.BackgroundAttributes
import com.harucut.frame.enums.ComponentType
import com.harucut.frame.enums.FrameType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "프레임 생성/수정 요청 DTO")
data class FrameCreateRequest(

    @field:NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "프레임 제목", example = "봄 여행 4컷")
    val title: String,

    @Schema(description = "프레임 설명", example = "벚꽃 배경의 여행 프레임")
    val description: String? = null,

    @field:NotBlank(message = "주소는 필수입니다.")
    @Schema(description = "프리뷰 리소스 key", example = "frames/preview/frame-1.png")
    val previewKey: String,

    @field:NotNull(message = "프레임 타입은 필수입니다.")
    @Schema(description = "프레임 타입", example = "CLASSIC")
    val frameType: FrameType,

    @Schema(description = "캔버스 너비(미저장, 호환용)", example = "1080")
    val canvasWidth: Int = 0,

    @Schema(description = "캔버스 높이(미저장, 호환용)", example = "1920")
    val canvasHeight: Int = 0,

    @field:NotNull(message = "배경 정보는 필수입니다.")
    @Schema(description = "배경 속성")
    val background: BackgroundAttributes,

    @field:Valid
    @Schema(description = "프레임 컴포넌트 목록")
    val components: List<ComponentRequest>? = null
) {
    @Schema(description = "프레임 컴포넌트 요청 DTO")
    data class ComponentRequest(
        @Schema(description = "클라이언트 컴포넌트 식별자", example = "comp-1")
        val id: String? = null,

        @field:NotNull
        @Schema(description = "컴포넌트 타입", example = "PHOTO")
        val type: ComponentType,

        @field:NotBlank
        @Schema(description = "소스 URL 또는 key", example = "s3://bucket/frames/item.png")
        val source: String,

        @Schema(description = "X 좌표", example = "120.5") val x: Double = 0.0,
        @Schema(description = "Y 좌표", example = "220.0") val y: Double = 0.0,
        @Schema(description = "너비", example = "360.0") val width: Double? = null,
        @Schema(description = "높이", example = "480.0") val height: Double? = null,
        @Schema(description = "스케일", example = "1.0") val scale: Double? = null,
        @Schema(description = "회전 각도", example = "0.0") val rotation: Double = 0.0,
        @Schema(description = "레이어 순서", example = "1") val zIndex: Int = 0,
        @Schema(description = "스타일 JSON 맵") val styleJson: Map<String, Any>? = null
    )
}

@Schema(description = "프레임 조회 응답 DTO")
data class FrameResponse(
    @Schema(description = "프레임 ID", example = "1") val frameId: Long?,
    @Schema(description = "프레임 제목", example = "봄 여행 4컷") val title: String,
    @Schema(description = "프레임 설명", example = "벚꽃 배경의 여행 프레임") val description: String?,
    @Schema(description = "프레임 소스(프리뷰 URL)", example = "https://.../frame-1.png") val source: String?,
    @Schema(description = "프레임 타입", example = "CLASSIC") val frameType: FrameType,
    @Schema(description = "배경 속성") val background: BackgroundAttributes,
    @Schema(description = "컴포넌트 목록") val components: List<ComponentResponse>,
    @Schema(description = "기본 제공(시스템) 프레임 여부", example = "false") val isSystem: Boolean
) {
    @Schema(description = "프레임 컴포넌트 응답 DTO")
    data class ComponentResponse(
        @Schema(description = "컴포넌트 ID", example = "10") val id: Long?,
        @Schema(description = "컴포넌트 타입", example = "PHOTO") val type: ComponentType,
        @Schema(description = "소스 URL", example = "https://.../item.png") val source: String?,
        @Schema(description = "리소스 key", example = "uploads/frames/item.png") val key: String,
        @Schema(description = "X 좌표", example = "120.5") val x: Double,
        @Schema(description = "Y 좌표", example = "220.0") val y: Double,
        @Schema(description = "너비", example = "360.0") val width: Double,
        @Schema(description = "높이", example = "480.0") val height: Double,
        @Schema(description = "회전 각도", example = "0.0") val rotation: Double,
        @Schema(description = "레이어 순서", example = "1") val zIndex: Int,
        @Schema(description = "스타일 JSON 맵") val style: Map<String, Any>
    )
}