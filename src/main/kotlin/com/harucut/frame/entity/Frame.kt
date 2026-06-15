package com.harucut.frame.entity

import com.harucut.frame.converter.BackgroundConverter
import com.harucut.frame.entity.attributes.BackgroundAttributes
import com.harucut.frame.enums.FrameType
import com.harucut.user.entity.User
import com.harucut.util.entity.BasePublicIdEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "frame")
class Frame : BasePublicIdEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "frame_id")
    var id: Long? = null

    @Column(nullable = false)
    lateinit var title: String

    @Column(nullable = false)
    lateinit var description: String

    @Column(nullable = false, length = 1024)
    lateinit var previewKey: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var frameType: FrameType

    @Convert(converter = BackgroundConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    lateinit var background: BackgroundAttributes

    @Column(name = "last_accessed_at")
    var lastAccessedAt: LocalDateTime? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @OneToMany(mappedBy = "frame", cascade = [CascadeType.ALL], orphanRemoval = true)
    var components: MutableList<FrameComponent> = mutableListOf()

    // ── 연관관계 편의 ─────────────────────────────────────
    fun assignUser(user: User) {
        this.user = user
    }

    fun addComponent(component: FrameComponent) {
        components.add(component)
        component.assignFrame(this)
    }

    // ── 상태 변경 ─────────────────────────────────────────
    fun updateMetadata(
        title: String,
        description: String,
        background: BackgroundAttributes,
        previewKey: String
    ) {
        this.title = title
        this.description = description
        this.background = background
        this.previewKey = previewKey
    }

    fun markAccessed(now: LocalDateTime = LocalDateTime.now()) {
        lastAccessedAt = now
    }

    // ── 팩토리 ────────────────────────────────────────────
    companion object {
        fun create(
            user: User,
            title: String,
            description: String,
            previewKey: String,
            frameType: FrameType,
            background: BackgroundAttributes
        ): Frame = Frame().apply {
            this.user = user
            this.title = title
            this.description = description
            this.previewKey = previewKey
            this.frameType = frameType
            this.background = background
        }
    }
}