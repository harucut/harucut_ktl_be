package com.harucut.notice.entity

import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "notice",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_notice_public_id", columnNames = ["public_id"])
    ]
)
class Notice(
    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false)
    var pinned: Boolean = false
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    @Column(nullable = false)
    var published: Boolean = false
        protected set

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
        protected set

    // 제목/본문/상단고정 여부 갱신
    fun update(title: String, content: String, pinned: Boolean) {
        this.title = title
        this.content = content
        this.pinned = pinned
    }

    // 게시
    fun publish(now: LocalDateTime) {
        this.published = true
        this.publishedAt = now
    }

    // 게시 취소 (재게시 시 publish(now)가 새 게시 시각을 기록하도록 초기화)
    fun unpublish() {
        this.published = false
        this.publishedAt = null
    }
}
