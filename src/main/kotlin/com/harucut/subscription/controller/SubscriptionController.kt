package com.harucut.subscription.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.subscription.service.SubscriptionService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Subscription", description = "내 구독 조회 및 해지 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @Operation(summary = "내 구독 조회", description = "로그인한 사용자의 구독 상태(요금제/기간/자동갱신 여부)를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "구독 없음(SUBS-004)")
    )
    @GetMapping
    fun getMySubscription(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<SubscriptionResponse>> {
        return Response.ok(subscriptionService.getMySubscription(principal.id!!)).toResponseEntity()
    }

    @Operation(
        summary = "자동갱신 해지",
        description = "다음 결제 주기부터 자동갱신을 중단합니다. 이미 결제한 기간의 만료 시점까지는 현재 요금제가 유지됩니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "해지 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "구독 없음(SUBS-004)"),
        ApiResponse(responseCode = "409", description = "이미 해지됨(SUBS-005)")
    )
    @PostMapping("/cancel")
    fun cancelAutoRenew(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        subscriptionService.cancelAutoRenew(principal.id!!)
        return Response.ok().toResponseEntity()
    }
}
