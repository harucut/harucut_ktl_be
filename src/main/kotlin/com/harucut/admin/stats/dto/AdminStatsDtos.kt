package com.harucut.admin.stats.dto

import com.harucut.admin.stats.enums.Granularity
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "매출 시계열 버킷")
data class RevenuePoint(
    @Schema(description = "집계 버킷(granularity=DAY면 yyyy-MM-dd, MONTH면 yyyy-MM)", example = "2026-07-01")
    val bucket: String,
    @Schema(description = "해당 버킷 매출 합계", example = "39000")
    val amount: Long,
    @Schema(description = "해당 버킷 승인 결제 건수", example = "10")
    val count: Long
)

@Schema(description = "기간별 매출 통계 응답")
data class RevenueSummaryResponse(
    @Schema(description = "조회 시작일", example = "2026-07-01")
    val from: LocalDate,
    @Schema(description = "조회 종료일", example = "2026-07-22")
    val to: LocalDate,
    @Schema(description = "집계 단위", example = "DAY")
    val granularity: Granularity,
    @Schema(description = "총 매출", example = "390000")
    val totalAmount: Long,
    @Schema(description = "총 승인 결제 건수", example = "100")
    val totalCount: Long,
    @Schema(description = "신규 결제(INITIAL) 매출", example = "200000")
    val initialAmount: Long,
    @Schema(description = "갱신 결제(RENEWAL) 매출", example = "190000")
    val renewalAmount: Long,
    @Schema(description = "기간별 매출 시계열")
    val series: List<RevenuePoint>
)

@Schema(description = "현재 구독 스냅샷 응답")
data class SubscriptionSnapshotResponse(
    @Schema(description = "tier별 활성 유료 구독 수(BASIC 제외)")
    val byTier: Map<PlanTier, Long>,
    @Schema(description = "status별 구독 수")
    val byStatus: Map<SubscriptionStatus, Long>,
    @Schema(description = "자동갱신이 켜진 구독 수", example = "80")
    val autoRenewOn: Long,
    @Schema(description = "자동갱신이 꺼진 구독 수", example = "20")
    val autoRenewOff: Long
)
