package com.harucut.storage.strategy

import com.harucut.storage.enums.UploadType

interface UploadPathStrategy {

    val uploadType: UploadType

    fun generateKey(publicId: String, originalFilename: String): String

    fun extractExtension(originalFilename: String): String {
        val index = originalFilename.lastIndexOf(".")
        return if (index > 0) originalFilename.substring(index) else ""
    }
}