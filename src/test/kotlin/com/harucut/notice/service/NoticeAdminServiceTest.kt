package com.harucut.notice.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.notice.entity.Notice
import com.harucut.notice.exception.NoticeErrorCode
import com.harucut.notice.repository.NoticeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class NoticeAdminServiceTest {

    private val noticeRepository = mockk<NoticeRepository>()
    private val fixedClock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
    private val service = NoticeAdminServiceImpl(noticeRepository, fixedClock)

    private fun notice(title: String = "제목", content: String = "본문", pinned: Boolean = false): Notice =
        Notice(title = title, content = content, pinned = pinned)

    private fun noticeWithId(id: Long, title: String, published: Boolean = false): Notice =
        mockk<Notice>(relaxed = true).also {
            every { it.id } returns id
            every { it.publicId } returns "public-$id"
            every { it.title } returns title
            every { it.content } returns "본문"
            every { it.pinned } returns false
            every { it.published } returns published
            every { it.publishedAt } returns null
        }

    @Nested
    inner class CreateNotice {

        @Test
        @DisplayName("공지를 미게시 상태로 생성한다")
        fun success() {
            val slot = slot<Notice>()
            every { noticeRepository.save(capture(slot)) } answers { slot.captured }

            service.createNotice("제목", "본문", true)

            assertThat(slot.captured.title).isEqualTo("제목")
            assertThat(slot.captured.content).isEqualTo("본문")
            assertThat(slot.captured.pinned).isTrue()
            assertThat(slot.captured.published).isFalse()
        }
    }

    @Nested
    inner class UpdateNotice {

        @Test
        @DisplayName("존재하는 공지의 제목/본문/고정 여부를 수정한다")
        fun success() {
            val n = notice()
            every { noticeRepository.findById(1L) } returns Optional.of(n)

            service.updateNotice(1L, "새 제목", "새 본문", true)

            assertThat(n.title).isEqualTo("새 제목")
            assertThat(n.content).isEqualTo("새 본문")
            assertThat(n.pinned).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 공지면 NOTICE_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { noticeRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.updateNotice(1L, "제목", "본문", false) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)
        }
    }

    @Nested
    inner class PublishNotice {

        @Test
        @DisplayName("공지를 게시 상태로 전환하고 게시 일시를 기록한다")
        fun success() {
            val n = notice()
            every { noticeRepository.findById(1L) } returns Optional.of(n)

            service.publishNotice(1L)

            assertThat(n.published).isTrue()
            assertThat(n.publishedAt).isEqualTo(java.time.LocalDateTime.now(fixedClock))
        }

        @Test
        @DisplayName("존재하지 않는 공지면 NOTICE_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { noticeRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.publishNotice(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)
        }
    }

    @Nested
    inner class UnpublishNotice {

        @Test
        @DisplayName("게시된 공지를 미게시 상태로 전환하고 게시 일시를 초기화한다")
        fun success() {
            val n = notice()
            n.publish(java.time.LocalDateTime.now(fixedClock))
            every { noticeRepository.findById(1L) } returns Optional.of(n)

            service.unpublishNotice(1L)

            assertThat(n.published).isFalse()
            assertThat(n.publishedAt).isNull()
        }

        @Test
        @DisplayName("게시 취소 후 재게시하면 새 게시 시각을 기록한다")
        fun republish() {
            val n = notice()
            every { noticeRepository.findById(1L) } returns Optional.of(n)
            service.publishNotice(1L)
            service.unpublishNotice(1L)
            assertThat(n.publishedAt).isNull()

            val laterClock = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC)
            val laterService = NoticeAdminServiceImpl(noticeRepository, laterClock)

            laterService.publishNotice(1L)

            assertThat(n.published).isTrue()
            assertThat(n.publishedAt).isEqualTo(java.time.LocalDateTime.now(laterClock))
        }
    }

    @Nested
    inner class DeleteNotice {

        @Test
        @DisplayName("존재하는 공지를 삭제한다")
        fun success() {
            val n = notice()
            every { noticeRepository.findById(1L) } returns Optional.of(n)
            every { noticeRepository.delete(n) } returns Unit

            service.deleteNotice(1L)

            verify { noticeRepository.delete(n) }
        }

        @Test
        @DisplayName("존재하지 않는 공지면 NOTICE_NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { noticeRepository.findById(1L) } returns Optional.empty()

            assertThatThrownBy { service.deleteNotice(1L) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)

            verify(exactly = 0) { noticeRepository.delete(any()) }
        }
    }

    @Nested
    inner class ListAllNotices {

        @Test
        @DisplayName("미게시 공지를 포함한 전체 목록을 페이지로 반환한다")
        fun success() {
            val published = noticeWithId(1L, "게시글", published = true)
            val unpublished = noticeWithId(2L, "미게시글")
            every { noticeRepository.findAll(any<org.springframework.data.domain.Pageable>()) } returns
                    PageImpl(listOf(published, unpublished), PageRequest.of(0, 10), 2)

            val result = service.listAllNotices(0, 10)

            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.title }).containsExactly("게시글", "미게시글")
        }

        @Test
        @DisplayName("page가 음수면 INVALID_INPUT_VALUE 예외를 던진다")
        fun invalidPage() {
            assertThatThrownBy { service.listAllNotices(-1, 10) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
        }
    }
}
