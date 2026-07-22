package com.harucut.notice.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.notice.dto.NoticeAdminResponse
import com.harucut.notice.entity.Notice
import com.harucut.notice.exception.NoticeErrorCode
import com.harucut.notice.repository.NoticeRepository
import com.harucut.util.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class NoticeAdminServiceImpl(
    private val noticeRepository: NoticeRepository,
    private val clock: Clock
) : NoticeAdminService {

    override fun createNotice(title: String, content: String, pinned: Boolean) {
        noticeRepository.save(Notice(title = title, content = content, pinned = pinned))
    }

    override fun updateNotice(noticeId: Long, title: String, content: String, pinned: Boolean) {
        getNoticeById(noticeId).update(title, content, pinned)
    }

    override fun publishNotice(noticeId: Long) {
        getNoticeById(noticeId).publish(LocalDateTime.now(clock))
    }

    override fun unpublishNotice(noticeId: Long) {
        getNoticeById(noticeId).unpublish()
    }

    override fun deleteNotice(noticeId: Long) {
        noticeRepository.delete(getNoticeById(noticeId))
    }

    @Transactional(readOnly = true)
    override fun listAllNotices(page: Int, size: Int): PageResponse<NoticeAdminResponse> {
        val pageable = createPageable(page, size)
        val noticePage = noticeRepository.findAll(pageable)
        return PageResponse.from(noticePage.map { NoticeAdminResponse.from(it) })
    }

    private fun getNoticeById(noticeId: Long): Notice =
        noticeRepository.findById(noticeId).orElseThrow { BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND) }

    private fun createPageable(page: Int, size: Int): Pageable {
        if (page < 0) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "page must be 0 or greater.")
        if (size < 1) throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "size must be 1 or greater.")
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    }
}
