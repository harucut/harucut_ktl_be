package com.harucut.notice.service

import com.harucut.notice.dto.NoticeResponse
import com.harucut.util.response.PageResponse

interface NoticeService {
    fun getPublishedNotices(page: Int, size: Int): PageResponse<NoticeResponse>
    fun getPublishedNotice(publicId: String): NoticeResponse
}
