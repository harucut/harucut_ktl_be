package com.harucut.storage.strategy

import com.harucut.storage.enums.UploadType
import org.springframework.stereotype.Component
import java.util.*

// 프로필 사진
@Component
class ProfileUploadPathStrategy : UploadPathStrategy {

    override val uploadType = UploadType.PROFILE

    override fun generateKey(publicId: String, originalFilename: String): String {
        val uniqueName = "${UUID.randomUUID()}${extractExtension(originalFilename)}"
        return "uploads/users/$publicId/profile/$uniqueName"
    }
}