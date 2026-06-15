package com.harucut.user.entity

import com.harucut.auth.oauth2.enums.Provider
import com.harucut.frame.entity.Frame
import com.harucut.subscription.entity.UserSubscription
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.util.entity.BasePublicIdEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_provider_email", columnNames = ["provider", "email"]),
        UniqueConstraint(name = "uk_provider_provider_id", columnNames = ["provider", "provider_id"]),
    ],
    indexes = [Index(name = "idx_user_public_id", columnList = "public_id")]
)
class User : BasePublicIdEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var provider: Provider

    @Column(name = "provider_id", length = 64)
    var providerId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var userRole: UserRole

    @Column(nullable = false)
    lateinit var email: String

    var password: String? = null

    @Column(nullable = false)
    lateinit var username: String

    @Column(nullable = false, length = 1024)
    lateinit var profileUrl: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var userStatus: UserStatus

    var deleteRequestedAt: LocalDateTime? = null

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var frames: MutableList<Frame> = mutableListOf()

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var subscription: UserSubscription? = null

    // ── 연관관계 편의 ──────────────────────────────────────
    fun addFrame(frame: Frame) {
        frames.add(frame)
        frame.assignUser(this)
    }

    fun removeFrame(frame: Frame) {
        frames.remove(frame)
    }

    fun attachSubscription(subscription: UserSubscription) {
        this.subscription = subscription
    }

    // ── 상태 변경 ─────────────────────────────────────────
    fun requestDeletion() {
        userStatus = UserStatus.DELETED_REQUESTED
        deleteRequestedAt = LocalDateTime.now()
    }

    fun reActivate() {
        userStatus = UserStatus.ACTIVE
        deleteRequestedAt = null
    }

    fun anonymize() {
        val uid = requireNotNull(id) { "persist 이후에만 anonymize 가능" }
        userStatus = UserStatus.DELETED
        email = "deleted_${uid}@harucut.local"
        username = "탈퇴한 사용자"
        password = null
        profileUrl = "resources/defaults/userDefaultImage.png"
        providerId = null
    }

    fun isReadyForDeletion(now: LocalDateTime): Boolean {
        if (userStatus != UserStatus.DELETED_REQUESTED || deleteRequestedAt == null) return false
        return deleteRequestedAt!!.plusDays(7).isBefore(now)
    }

    fun changePassword(password: String) {
        this.password = password
    }

    fun changeUsername(username: String) {
        this.username = username
    }

    fun changeProfileUrl(profileUrl: String) {
        this.profileUrl = profileUrl
    }

    // ── 팩토리 ──────────────────────────────────────────
    companion object {
        fun social(
            provider: Provider,
            providerId: String,
            email: String,
            username: String,
            profileUrl: String
        ): User = User().apply {
            this.provider = provider
            this.providerId = providerId
            this.userRole = UserRole.ROLE_USER
            this.email = email
            this.username = username
            this.profileUrl = profileUrl
            this.userStatus = UserStatus.ACTIVE
        }

        fun local(
            email: String,
            encodedPassword: String,
            username: String,
            profileUrl: String
        ): User = User().apply {
            this.provider = Provider.HARUCUT
            this.userRole = UserRole.ROLE_USER
            this.email = email
            this.password = encodedPassword
            this.username = username
            this.profileUrl = profileUrl
            this.userStatus = UserStatus.ACTIVE
        }
    }
}