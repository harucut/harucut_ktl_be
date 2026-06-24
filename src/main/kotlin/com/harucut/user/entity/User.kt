package com.harucut.user.entity

import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.util.component.generatePublicId
import com.harucut.util.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: Provider,

    @Column(name = "provider_id", length = 64)
    var providerId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var userRole: UserRole,

    @Column(nullable = false)
    var email: String,

    var password: String? = null,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false, length = 1024)
    var profileImageUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var userStatus: UserStatus,

    var deleteRequestedAt: LocalDateTime? = null
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long? = null

    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    var publicId: String = generatePublicId()
        protected set

    // ── 연관관계 편의 메서드 ──────────────────────────────

    // ── 상태 변경 ─────────────────────────────────────────

    fun changePassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    // 닉네임(username) 변경
    fun changeUsername(username: String) {
        this.username = username
    }

    // 프로필 이미지 key/URL 변경
    fun changeProfileImageUrl(profileImageUrl: String) {
        this.profileImageUrl = profileImageUrl
    }

    fun deleteRequested() {
        this.userStatus = UserStatus.DELETED_REQUESTED
        this.deleteRequestedAt = LocalDateTime.now()
    }

    fun reActivate() {
        this.userStatus = UserStatus.ACTIVE
        this.deleteRequestedAt = null
    }

    fun delete() {
        this.userStatus = UserStatus.DELETED
        this.email = "deleted_${id}@harucut.local"
        this.username = "탈퇴한 사용자"
        this.password = null
        this.profileImageUrl = DEFAULT_PROFILE_IMAGE
        this.providerId = null
    }

    fun isReadyForDeletion(now: LocalDateTime): Boolean {
        val requestedAt = deleteRequestedAt ?: return false
        return userStatus == UserStatus.DELETED_REQUESTED &&
                requestedAt.plusDays(DELETION_GRACE_DAYS).isBefore(now)
    }

    companion object {
        const val DELETION_GRACE_DAYS = 7L
        private const val DEFAULT_PROFILE_IMAGE = "resources/defaults/userDefaultImage.png"
    }
}