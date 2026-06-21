package com.harucut.storage.service

import com.harucut.storage.dto.PresignedUploadResponse
import com.harucut.storage.enums.ContentType
import com.harucut.storage.enums.UploadType

interface FileStorageService {

    /** S3 객체 삭제 */
    fun delete(key: String)

    /** 조회용 presigned GET URL */
    fun generatePresignedGetUrl(key: String): String

    /** 다운로드 강제 presigned URL (downloadFileName 미지정 시 key에서 파일명 추출) */
    fun generatePresignedDownloadUrl(key: String, downloadFileName: String? = null): String

    /** 클라이언트가 S3에 직접 PUT 업로드 하기 위한 presigned URL 발급 */
    fun generatePresignedUploadUrl(
        uploadType: UploadType,
        originalFilename: String,
        contentType: ContentType,
        publicId: String,
        isTemp: Boolean
    ): PresignedUploadResponse

    /** temp → 영구 경로 승격 (copy 후 원본 delete) */
    fun moveFile(sourceKey: String, destinationKey: String): String
}