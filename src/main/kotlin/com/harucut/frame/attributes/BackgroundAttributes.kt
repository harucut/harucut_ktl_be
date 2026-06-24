package com.harucut.frame.attributes

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.harucut.frame.enums.BackgroundType

// 배경 속성 다형성 루트 — JSON "type" 판별자로 하위 타입 매핑 (레거시 JSON 포맷 유지)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ColorBackgroundAttributes::class, name = "COLOR"),
    JsonSubTypes.Type(value = ImageBackgroundAttributes::class, name = "IMAGE"),
    JsonSubTypes.Type(value = VideoBackgroundAttributes::class, name = "VIDEO")
)
sealed class BackgroundAttributes {
    abstract val type: BackgroundType
}

// 단색 배경 — HEX 색상값
data class ColorBackgroundAttributes(
    val value: String
) : BackgroundAttributes() {
    override val type: BackgroundType = BackgroundType.COLOR
}

// 이미지 배경 — S3 key + 불투명도
data class ImageBackgroundAttributes(
    val key: String,
    val opacity: Double
) : BackgroundAttributes() {
    override val type: BackgroundType = BackgroundType.IMAGE
}

// 비디오 배경 — S3 key + 자동재생/반복 여부
data class VideoBackgroundAttributes(
    val key: String,
    val autoPlay: Boolean,
    val loop: Boolean
) : BackgroundAttributes() {
    override val type: BackgroundType = BackgroundType.VIDEO
}