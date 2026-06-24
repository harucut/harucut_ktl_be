package com.harucut.subscription.entity

import java.time.LocalDateTime

// 사용량 사이클 스냅샷 (표시/계산용 비영속 값 객체)
data class CycleView(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val videoUploadCount: Int
)
