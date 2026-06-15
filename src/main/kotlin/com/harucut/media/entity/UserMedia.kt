package com.harucut.media.entity

import com.harucut.media.enums.UserMediaType
import com.harucut.user.entity.User
import com.harucut.util.entity.BasePublicIdEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "user_media",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_media_s3_key", columnNames = ["s3_key"])],
    indexes = [
        Index(name = "idx_user_media_public_id", columnList = "public_id"),
        Index(name = "idx_user_media_user_id",   columnList = "user_id"),
        Index(name = "idx_user_media_type",       columnList = "media_type")
    ]
)
class UserMedia : BasePublicIdEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 32)
    lateinit var mediaType: UserMediaType

    @Column(name = "s3_key", nullable = false, length = 512)
    lateinit var s3Key: String

    @Column(name = "original_s3_key", length = 512)
    var originalS3Key: String? = null

    @Column(name = "original_file_name", length = 255)
    var originalFileName: String? = null

    @Column(name = "display_name", nullable = false, length = 255)
    lateinit var displayName: String

    @Column(name = "transcode_job_id", length = 128)
    var transcodeJobId: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    fun changeDisplayName(displayName: String) { this.displayName = displayName }

    companion object {
        fun ofPhoto(user: User, s3Key: String, displayName: String): UserMedia =
            UserMedia().apply {
                this.user        = user
                this.mediaType   = UserMediaType.PHOTO
                this.s3Key       = s3Key
                this.displayName = displayName
            }

        fun ofVideo(
            user: User,
            s3Key: String,
            originalS3Key: String,
            originalFileName: String,
            displayName: String,
            transcodeJobId: String
        ): UserMedia = UserMedia().apply {
            this.user             = user
            this.mediaType        = UserMediaType.VIDEO
            this.s3Key            = s3Key
            this.originalS3Key    = originalS3Key
            this.originalFileName = originalFileName
            this.displayName      = displayName
            this.transcodeJobId   = transcodeJobId
        }
    }
}
