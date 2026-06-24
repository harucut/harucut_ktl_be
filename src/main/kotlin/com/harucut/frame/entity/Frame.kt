package com.harucut.frame.entity

import com.harucut.frame.attributes.BackgroundAttributes
import com.harucut.frame.converter.BackgroundConverter
import com.harucut.frame.enums.FrameType
import com.harucut.user.entity.User
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "frame")
class Frame(

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var description: String,

    @Column(name = "preview_key", nullable = false, length = 1024)
    var previewKey: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "frame_type", nullable = false, length = 32)
    val frameType: FrameType,

    @Convert(converter = BackgroundConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    var background: BackgroundAttributes,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "frame_id")
    val id: Long? = null

    @OneToMany(mappedBy = "frame", cascade = [CascadeType.ALL], orphanRemoval = true)
    val components: MutableList<FrameComponent> = mutableListOf()

    // 컴포넌트 추가 + 양방향 연관관계 동기화
    fun addComponent(component: FrameComponent) {
        components.add(component)
        component.assignFrame(this)
    }

    // 기존 컴포넌트 전부 제거 (orphanRemoval로 삭제)
    fun clearComponents() {
        components.clear()
    }

    // 제목/설명/배경/프리뷰 메타데이터 갱신
    fun updateMetadata(title: String, description: String, background: BackgroundAttributes, previewKey: String) {
        this.title = title
        this.description = description
        this.background = background
        this.previewKey = previewKey
    }
}