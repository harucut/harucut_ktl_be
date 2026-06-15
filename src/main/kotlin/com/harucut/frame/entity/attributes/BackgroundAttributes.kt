package com.harucut.frame.entity.attributes

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.harucut.frame.enums.BackgroundType

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
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

data class ColorBackgroundAttributes(
    override val type: BackgroundType = BackgroundType.COLOR,
    val value: String = ""
) : BackgroundAttributes()

data class ImageBackgroundAttributes(
    override val type: BackgroundType = BackgroundType.IMAGE,
    val key: String = "",
    val opacity: Double = 1.0
) : BackgroundAttributes()

data class VideoBackgroundAttributes(
    override val type: BackgroundType = BackgroundType.VIDEO,
    val key: String = "",
    val autoPlay: Boolean = false,
    val loop: Boolean = false
) : BackgroundAttributes()