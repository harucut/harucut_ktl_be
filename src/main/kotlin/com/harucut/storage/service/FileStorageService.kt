package com.harucut.storage.service

import com.harucut.storage.dto.PresignedUploadResponse
import com.harucut.storage.enums.ContentType
import com.harucut.storage.enums.UploadType

interface FileStorageService {

    fun delete(key: String)

    fun generatePresignedGetUrl(key: String): String

    fun generatePresignedDownloadUrl(key: String, downloadFileName: String? = null): String

    fun generatePresignedUploadUrl(
        uploadType: UploadType,
        originalFilename: String,
        contentType: ContentType,
        publicId: String
    ): PresignedUploadResponse
}
