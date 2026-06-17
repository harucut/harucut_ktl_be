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
}