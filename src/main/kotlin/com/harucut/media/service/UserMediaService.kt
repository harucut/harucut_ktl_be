package com.harucut.media.service

import com.harucut.media.dto.UserMediaDisplayNameUpdateRequest
import com.harucut.media.dto.UserMediaRegisterRequest
import com.harucut.media.dto.UserMediaResponse
import com.harucut.util.response.PageResponse

interface UserMediaService {

    fun registerMedia(userId: Long, request: UserMediaRegisterRequest): UserMediaResponse

    fun getMyMedia(userId: Long, page: Int, size: Int): PageResponse<UserMediaResponse>

    fun getDownloadUrl(userId: Long, mediaId: Long): String

    fun updateDisplayName(userId: Long, mediaId: Long, request: UserMediaDisplayNameUpdateRequest): UserMediaResponse
}