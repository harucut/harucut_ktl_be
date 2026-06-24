package com.harucut.frame.entity

import com.harucut.frame.enums.ComponentType
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "frame_component")
class FrameComponent(

    @Column(nullable = false, length = 1024)
    val source: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val type: ComponentType,

    val x: Double,
    val y: Double,
    val width: Double?,
    val height: Double?,
    val scale: Double?,
    val rotation: Double,

    @Column(name = "z_index")
    val zIndex: Int,

    @Column(name = "style_json", columnDefinition = "json")
    val styleJson: String?
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "frame_component_id")
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_id")
    var frame: Frame? = null

    // 부모 프레임 연관관계 설정
    fun assignFrame(frame: Frame) {
        this.frame = frame
    }
}