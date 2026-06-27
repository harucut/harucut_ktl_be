package com.harucut.storage.enums

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode

enum class ContentType(
    val mimeType: String,
    val extensions: Set<String>
) {

    JPEG("image/jpeg", setOf("jpg", "jpeg")),
    PNG("image/png", setOf("png")),
    WEBP("image/webp", setOf("webp")),
    GIF("image/gif", setOf("gif"));

    companion object {
        fun validate(mimeType: String, extension: String?): ContentType {
            val ext = normalizeExt(extension)
            return entries.firstOrNull {
                it.mimeType.equals(mimeType, ignoreCase = true) && ext in it.extensions
            } ?: throw BusinessException(
                GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "지원하지 않는 MIME/확장자: $mimeType / $ext"
            )
        }

        private fun normalizeExt(extension: String?): String {
            if (extension.isNullOrBlank()) {
                throw BusinessException(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE, "확장자가 비어있습니다.")
            }
            return extension.trim().removePrefix(".").lowercase()
        }
    }
}
