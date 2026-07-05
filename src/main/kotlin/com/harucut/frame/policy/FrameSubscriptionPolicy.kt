package com.harucut.frame.policy

import com.harucut.user.entity.User
import java.time.LocalDateTime

// 프레임 도메인이 구독 정책에 요구하는 포트 (구현은 subscription 모듈)
interface FrameSubscriptionPolicy {

    // 현재 보관 중인 프레임 개수가 요금제 동시 보관 cap 이내인지 검증
    fun assertFrameRetentionLimit(user: User, currentFrameCount: Int)

    // 동시 보관 cap 개수 (무제한이면 null)
    fun resolveFrameRetentionCap(user: User): Int?

    // 보관 기간 cutoff 시각 (무제한이면 null)
    fun resolveHistoryCutoff(user: User): LocalDateTime?

    // createdAt이 보관 기간 내인지 검증
    fun assertHistoryAccessible(user: User, createdAt: LocalDateTime?)
}