package com.harucut.media.policy

import com.harucut.user.entity.User
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NoOpMediaSubscriptionPolicy : MediaSubscriptionPolicy {

    // 보관기간 무제한 (cutoff 없음)
    override fun resolveHistoryCutoff(user: User): LocalDateTime? = null

    // 내역 접근 항상 허용
    override fun assertHistoryAccessible(user: User, createdAt: LocalDateTime?) = Unit

    // 업로드 쿼터 무제한 통과
    override fun assertAndConsumeVideoUploadQuota(user: User) = Unit
}