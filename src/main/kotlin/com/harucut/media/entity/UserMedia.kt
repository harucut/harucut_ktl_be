package com.harucut.media.entity

import com.harucut.media.enums.UserMediaType
import com.harucut.user.entity.User
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "user_media",
)
class UserMedia(

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 32)
    val mediaType: UserMediaType,

    @Column(name = "s3_key", nullable = false, length = 512)
    val s3Key: String,

    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    val id: Long? = null

    fun changeDisplayName(displayName: String) {
        this.displayName = displayName
    }

    companion object {
        fun ofPhoto(user: User, s3Key: String, displayName: String): UserMedia =
            UserMedia(
                mediaType = UserMediaType.PHOTO,
                s3Key = s3Key,
                displayName = displayName,
                user = user
            )
    }
}