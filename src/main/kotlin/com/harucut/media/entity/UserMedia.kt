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

    @Column(name = "original_s3_key", length = 512)
    val originalS3Key: String? = null,

    @Column(name = "original_file_name", length = 255)
    val originalFileName: String? = null,

    @Column(name = "transcode_job_id", length = 128)
    val transcodeJobId: String? = null,

    @Column(name = "thumbnail_key", length = 512)
    var thumbnailKey: String? = null,

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

        fun ofVideo(
            user: User,
            s3Key: String,
            originalS3Key: String?,
            originalFileName: String?,
            displayName: String,
            transcodeJobId: String?,
            thumbnailKey: String? = null
        ): UserMedia =
            UserMedia(
                mediaType = UserMediaType.VIDEO,
                s3Key = s3Key,
                displayName = displayName,
                originalS3Key = originalS3Key,
                originalFileName = originalFileName,
                transcodeJobId = transcodeJobId,
                thumbnailKey = thumbnailKey,
                user = user
            )
    }
}