package com.harucut.storage.strategy

import com.harucut.storage.enums.UploadType
import org.springframework.stereotype.Component
import java.util.*

// 프레임 요소 (temp에 임시로 저장됨)

@Component
class FrameComponentUploadPathStrategy : UploadPathStrategy {

    override val uploadType = UploadType.FRAME_COMPONENT

    override fun generateKey(publicId: String, originalFilename: String, isTemp: Boolean): String {
        val rootPath = if (isTemp) TEMP_PATH_PREFIX else UPLOAD_PATH_PREFIX
        val uniqueName = "${UUID.randomUUID()}${extractExtension(originalFilename)}"
        return "$rootPath/users/$publicId/components/$uniqueName"
    }

    companion object {
        private const val TEMP_PATH_PREFIX = "temp"
        private const val UPLOAD_PATH_PREFIX = "uploads"
    }
}