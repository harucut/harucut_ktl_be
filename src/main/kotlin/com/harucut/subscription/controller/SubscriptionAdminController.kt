package com.harucut.subscription.controller

import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.service.SubscriptionAdminService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import com.harucut.subscription.plan.PlanTier
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Subscription Admin", description = "관리자 구독 운영 오버라이드 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/subscriptions")
class SubscriptionAdminController(
    private val subscriptionAdminService: SubscriptionAdminService
) {

    @Operation(summary = "사용자 구독 조회", description = "관리자가 특정 사용자의 구독 상태를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "구독 없음")
    )
    @GetMapping("/{userId}")
    fun getSubscription(
        @Parameter(description = "사용자 ID", required = true) @PathVariable userId: Long
    ): ResponseEntity<Response<SubscriptionResponse>> {
        return Response.ok(subscriptionAdminService.getSubscription(userId)).toResponseEntity()
    }

    @Operation(
        summary = "요금제 강제 변경",
        description = "결제 없이 관리자가 사용자의 요금제 단계를 직접 변경합니다. 운영 오버라이드 전용입니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "변경 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @PatchMapping("/{userId}/plan")
    fun changePlan(
        @Parameter(description = "사용자 ID", required = true) @PathVariable userId: Long,
        @Parameter(description = "변경할 요금제 단계", required = true) @RequestParam planTier: PlanTier
    ): ResponseEntity<Response<Unit>> {
        subscriptionAdminService.changePlan(userId, planTier)
        return Response.ok().toResponseEntity()
    }
}
