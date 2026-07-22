package com.harucut.notice.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.notice.dto.NoticeResponse
import com.harucut.notice.exception.NoticeErrorCode
import com.harucut.notice.repository.NoticeRepository
import com.harucut.util.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NoticeServiceImpl(
    private val noticeRepository: NoticeRepository
) : NoticeService {

    // 게시된 공지만 상단고정 우선, 게시 최신순으로 조회
    override fun getPublishedNotices(page: Int, size: Int): PageResponse<NoticeResponse> {
        val pageable = createPageable(page, size)
        val noticePage = noticeRepository.findByPublishedTrueOrderByPinnedDescPublishedAtDesc(pageable)
        return PageResponse.from(noticePage.map { NoticeResponse.from(it) })
    }

    // 게시된 공지 단건 조회 (미게시/미존재는 404)
    override fun getPublishedNotice(publicId: String): NoticeResponse {
        val notice = noticeRepository.findByPublicId(publicId)
            ?.takeIf { it.published }
            ?: throw BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND)
        return NoticeResponse.from(notice)
    }

    private fun createPageable(page: Int, size: Int): Pageable {
        if (page < 0) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "page must be 0 or greater.")
        if (size < 1) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "size must be 1 or greater.")
        return PageRequest.of(page, size)
    }
}
