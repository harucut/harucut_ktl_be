package com.harucut.notice.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NoticeTest {

    private fun notice(): Notice = Notice(title = "제목", content = "본문")

    @Nested
    inner class Create {

        @Test
        @DisplayName("생성 시 미게시 상태로 시작한다")
        fun success() {
            val n = notice()

            assertThat(n.published).isFalse()
            assertThat(n.publishedAt).isNull()
        }
    }

    @Nested
    inner class Publish {

        @Test
        @DisplayName("게시하면 published=true와 게시 시각을 기록한다")
        fun success() {
            val n = notice()
            val now = LocalDateTime.now()

            n.publish(now)

            assertThat(n.published).isTrue()
            assertThat(n.publishedAt).isEqualTo(now)
        }
    }

    @Nested
    inner class Unpublish {

        @Test
        @DisplayName("게시 취소하면 published=false이고 publishedAt을 초기화한다")
        fun success() {
            val n = notice()
            n.publish(LocalDateTime.now())

            n.unpublish()

            assertThat(n.published).isFalse()
            assertThat(n.publishedAt).isNull()
        }

        @Test
        @DisplayName("게시 취소 후 재게시하면 새 게시 시각을 기록한다")
        fun republish() {
            val n = notice()
            n.publish(LocalDateTime.now().minusDays(1))
            n.unpublish()

            val newNow = LocalDateTime.now()
            n.publish(newNow)

            assertThat(n.published).isTrue()
            assertThat(n.publishedAt).isEqualTo(newNow)
        }
    }
}
