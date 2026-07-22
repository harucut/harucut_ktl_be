package com.harucut.notice.service

import com.harucut.notice.dto.NoticeAdminResponse
import com.harucut.util.response.PageResponse

interface NoticeAdminService {
    fun createNotice(title: String, content: String, pinned: Boolean)
    fun updateNotice(noticeId: Long, title: String, content: String, pinned: Boolean)
    fun publishNotice(noticeId: Long)
    fun unpublishNotice(noticeId: Long)
    fun deleteNotice(noticeId: Long)
    fun listAllNotices(page: Int, size: Int): PageResponse<NoticeAdminResponse>
}
