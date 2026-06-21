package com.harucut.storage.strategy

import com.harucut.storage.enums.UploadType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FourcutVideoUploadPathStrategy : UploadPathStrategy {

    override val uploadType = UploadType.FOURCUT_VIDEO

    override fun generateKey(publicId: String, originalFilename: String, isTemp: Boolean): String {
        val uniqueName = "${UUID.randomUUID()}${extractExtension(originalFilename)}"
        return "uploads/users/$publicId/webm/$uniqueName"
    }
}