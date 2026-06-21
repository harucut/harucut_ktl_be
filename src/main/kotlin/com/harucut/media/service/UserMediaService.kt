package com.harucut.media.service

import com.harucut.media.dto.UserMediaDisplayNameUpdateRequest
import com.harucut.media.dto.UserMediaRegisterRequest
import com.harucut.media.dto.UserMediaResponse
import com.harucut.media.enums.UserMediaType
import com.harucut.util.response.PageResponse

interface UserMediaService {

    fun registerMedia(userId: Long, request: UserMediaRegisterRequest): UserMediaResponse

    fun getMyMedia(userId: Long, mediaType: UserMediaType?, page: Int, size: Int): PageResponse<UserMediaResponse>

    fun getDownloadUrl(userId: Long, mediaId: Long): String

    fun updateDisplayName(userId: Long, mediaId: Long, request: UserMediaDisplayNameUpdateRequest): UserMediaResponse

    fun saveTranscodedVideo(
        userPublicId: String,
        originalFileName: String?,
        outputS3Path: String,
        thumbnailOutputS3Path: String?,
        transcodeJobId: String
    ): UserMediaResponse
}