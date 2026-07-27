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

    // 시스템 프레임은 오너가 없다 (isSystem=true ⇔ user=null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    val user: User?,

    @Column(name = "is_system", nullable = false)
    val isSystem: Boolean = false
) : BaseEntity() {

    init {
        require(isSystem == (user == null)) {
            "시스템 프레임은 user가 없어야 하고, 사용자 프레임은 user가 있어야 합니다."
        }
    }

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

    companion object {
        // 오너 없는 시스템(기본 제공) 프레임 생성 팩토리
        fun system(
            title: String,
            description: String,
            previewKey: String,
            frameType: FrameType,
            background: BackgroundAttributes
        ): Frame = Frame(
            title = title,
            description = description,
            previewKey = previewKey,
            frameType = frameType,
            background = background,
            user = null,
            isSystem = true
        )
    }
}