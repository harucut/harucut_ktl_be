package com.harucut.notice.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.notice.entity.Notice
import com.harucut.notice.exception.NoticeErrorCode
import com.harucut.notice.repository.NoticeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class NoticeServiceTest {

    private val noticeRepository = mockk<NoticeRepository>()
    private val service = NoticeServiceImpl(noticeRepository)

    private fun noticeWithId(
        id: Long,
        title: String,
        published: Boolean = true,
        pinned: Boolean = false
    ): Notice =
        mockk<Notice>(relaxed = true).also {
            every { it.id } returns id
            every { it.publicId } returns "public-$id"
            every { it.title } returns title
            every { it.content } returns "본문"
            every { it.pinned } returns pinned
            every { it.published } returns published
            every { it.publishedAt } returns null
        }

    @Nested
    inner class GetPublishedNotices {

        @Test
        @DisplayName("상단고정·게시 최신순 정렬로 게시된 공지만 조회한다")
        fun success() {
            val pinnedFirst = noticeWithId(1L, "고정 공지", pinned = true)
            val second = noticeWithId(2L, "일반 공지", pinned = false)
            every { noticeRepository.findByPublishedTrueOrderByPinnedDescPublishedAtDesc(any<Pageable>()) } returns
                    PageImpl(listOf(pinnedFirst, second), PageRequest.of(0, 10), 2)

            val result = service.getPublishedNotices(0, 10)

            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.title }).containsExactly("고정 공지", "일반 공지")
        }

        @Test
        @DisplayName("page가 음수면 INVALID_INPUT_VALUE 예외를 던진다")
        fun invalidPage() {
            assertThatThrownBy { service.getPublishedNotices(-1, 10) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
        }
    }

    @Nested
    inner class GetPublishedNotice {

        @Test
        @DisplayName("게시된 공지를 publicId로 조회한다")
        fun success() {
            val n = noticeWithId(1L, "공지", published = true)
            every { noticeRepository.findByPublicId("public-1") } returns n

            val result = service.getPublishedNotice("public-1")

            assertThat(result.title).isEqualTo("공지")
        }

        @Test
        @DisplayName("존재하지 않으면 NOTICE_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { noticeRepository.findByPublicId("unknown") } returns null

            assertThatThrownBy { service.getPublishedNotice("unknown") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)
        }

        @Test
        @DisplayName("미게시 공지면 NOTICE_NOT_FOUND 예외를 던진다")
        fun unpublished() {
            val n = noticeWithId(1L, "공지", published = false)
            every { noticeRepository.findByPublicId("public-1") } returns n

            assertThatThrownBy { service.getPublishedNotice("public-1") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)
        }
    }
}
