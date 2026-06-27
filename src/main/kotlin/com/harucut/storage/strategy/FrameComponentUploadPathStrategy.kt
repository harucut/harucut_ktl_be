package com.harucut.storage.strategy

import com.harucut.storage.enums.UploadType
import org.springframework.stereotype.Component
import java.util.*

@Component
class FrameComponentUploadPathStrategy : UploadPathStrategy {

    override val uploadType = UploadType.FRAME_COMPONENT

    override fun generateKey(publicId: String, originalFilename: String): String {
        val uniqueName = "${UUID.randomUUID()}${extractExtension(originalFilename)}"
        return "uploads/users/$publicId/components/$uniqueName"
    }
}
