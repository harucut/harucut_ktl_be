package com.harucut.subscription.plan

import java.time.LocalDateTime

/** 내역 보관 정책 — 무제한 또는 N일 */
sealed interface Retention {

    fun cutoffFrom(now: LocalDateTime): LocalDateTime?
    fun isAccessible(createdAt: LocalDateTime?, now: LocalDateTime): Boolean

    data object Unlimited : Retention {
        override fun cutoffFrom(now: LocalDateTime): LocalDateTime? = null
        override fun isAccessible(createdAt: LocalDateTime?, now: LocalDateTime) = true
    }

    data class Days(val days: Long) : Retention {
        init {
            require(days > 0) { "days는 1 이상이어야 합니다: $days" }
        }

        override fun cutoffFrom(now: LocalDateTime): LocalDateTime = now.minusDays(days)
        override fun isAccessible(createdAt: LocalDateTime?, now: LocalDateTime): Boolean =
            createdAt == null || !createdAt.isBefore(cutoffFrom(now))
    }
}