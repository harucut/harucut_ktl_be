package com.harucut.admin.stats.controller

import com.harucut.admin.stats.dto.RevenueSummaryResponse
import com.harucut.admin.stats.dto.SubscriptionSnapshotResponse
import com.harucut.admin.stats.enums.Granularity
import com.harucut.admin.stats.service.AdminStatsService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Admin Stats", description = "관리자 매출·구독 통계 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/stats")
class AdminStatsController(
    private val adminStatsService: AdminStatsService
) {

    @Operation(
        summary = "기간별 매출 통계 조회",
        description = "승인된 결제(status=APPROVED)를 기준으로 기간별 매출 시계열과 합계, 신규/갱신 매출을 조회합니다. from/to를 생략하면 이번 달을 기본 범위로 사용합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "조회 범위가 잘못됨(from > to 또는 범위 상한 초과)"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @GetMapping("/revenue")
    fun getRevenue(
        @Parameter(description = "조회 시작일(생략 시 이번 달 1일)", example = "2026-07-01")
        @RequestParam(required = false) from: LocalDate?,
        @Parameter(description = "조회 종료일(생략 시 오늘)", example = "2026-07-22")
        @RequestParam(required = false) to: LocalDate?,
        @Parameter(description = "집계 단위, 기본값 DAY")
        @RequestParam(required = false, defaultValue = "DAY") granularity: Granularity
    ): ResponseEntity<Response<RevenueSummaryResponse>> {
        return Response.ok(adminStatsService.getRevenueSummary(from, to, granularity)).toResponseEntity()
    }

    @Operation(
        summary = "현재 구독 스냅샷 조회",
        description = "현재 시점 tier별 활성 유료 구독 수, status별 구독 수, 자동갱신 on/off 구독 수를 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @GetMapping("/subscriptions")
    fun getSubscriptionSnapshot(): ResponseEntity<Response<SubscriptionSnapshotResponse>> {
        return Response.ok(adminStatsService.getSubscriptionSnapshot()).toResponseEntity()
    }
}
