package com.harucut.admin.stats.service

import com.harucut.admin.stats.dto.RevenueSummaryResponse
import com.harucut.admin.stats.dto.SubscriptionSnapshotResponse
import com.harucut.admin.stats.enums.Granularity
import java.time.LocalDate

interface AdminStatsService {

    // from/to 생략 시 이번 달(1일 ~ 오늘)을 기본 범위로 사용한다.
    fun getRevenueSummary(from: LocalDate?, to: LocalDate?, granularity: Granularity): RevenueSummaryResponse

    fun getSubscriptionSnapshot(): SubscriptionSnapshotResponse
}
