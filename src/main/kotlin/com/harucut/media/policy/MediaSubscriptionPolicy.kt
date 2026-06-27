package com.harucut.media.policy

import com.harucut.user.entity.User
import java.time.LocalDateTime

interface MediaSubscriptionPolicy {

    /** 보관 기간 cutoff. null이면 무제한 */
    fun resolveHistoryCutoff(user: User): LocalDateTime?

    /** createdAt이 보관 기간 내인지 검증 */
    fun assertHistoryAccessible(user: User, createdAt: LocalDateTime?)
}