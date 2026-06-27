package com.harucut.storage.strategy

import com.harucut.storage.enums.UploadType
import org.springframework.stereotype.Component
import java.util.*

// 프레임

@Component
class FrameUploadPathStrategy : UploadPathStrategy {

    override val uploadType = UploadType.FRAME

    override fun generateKey(publicId: String, originalFilename: String): String {
        val uniqueName = "${UUID.randomUUID()}${extractExtension(originalFilename)}"
        return "uploads/users/$publicId/webm/$uniqueName"
    }
}