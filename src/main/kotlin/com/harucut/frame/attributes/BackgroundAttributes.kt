package com.harucut.frame.attributes

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.harucut.frame.enums.BackgroundType

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ColorBackgroundAttributes::class, name = "COLOR"),
    JsonSubTypes.Type(value = ImageBackgroundAttributes::class, name = "IMAGE")
)
sealed class BackgroundAttributes {
    abstract val type: BackgroundType
}

data class ColorBackgroundAttributes(
    val value: String
) : BackgroundAttributes() {
    override val type: BackgroundType = BackgroundType.COLOR
}

data class ImageBackgroundAttributes(
    val key: String,
    val opacity: Double
) : BackgroundAttributes() {
    override val type: BackgroundType = BackgroundType.IMAGE
}
