package com.harucut.frame.entity

import com.harucut.frame.enums.ComponentType
import com.harucut.util.entity.BasePublicIdEntity
import jakarta.persistence.*

@Entity
@Table(name = "frame_component")
class FrameComponent : BasePublicIdEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "frame_component_id")
    var id: Long? = null

    @Column(nullable = false, length = 1024)
    lateinit var source: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var type: ComponentType

    var x: Double = 0.0
    var y: Double = 0.0
    var width: Double? = null
    var height: Double? = null
    var scale: Double? = null
    var rotation: Double = 0.0
    var zIndex: Int = 0

    @Column(columnDefinition = "json")
    var styleJson: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_id")
    var frame: Frame? = null

    fun assignFrame(frame: Frame) {
        this.frame = frame
    }

    companion object {
        fun create(
            frame: Frame,
            source: String,
            type: ComponentType,
            x: Double,
            y: Double,
            zIndex: Int
        ): FrameComponent = FrameComponent().apply {
            this.frame = frame
            this.source = source
            this.type = type
            this.x = x
            this.y = y
            this.zIndex = zIndex
        }
    }
}