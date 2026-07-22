package com.harucut.payment.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.payment.dto.SubscribeRequest
import com.harucut.payment.service.PaymentService
import com.harucut.subscription.dto.SubscriptionResponse
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Payment", description = "결제 기반 구독 업그레이드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/payments")
class PaymentController(
    private val paymentService: PaymentService
) {

    @Operation(
        summary = "구독 결제",
        description = "빌링키를 발급하고 최초 결제에 성공하면 요청한 요금제로 구독을 활성화합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "결제 및 구독 활성화 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패 또는 BASIC 요금제 요청(PAY-007)"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "402", description = "결제 실패(PAY-002)"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자"),
        ApiResponse(responseCode = "409", description = "이미 유료 요금제 구독 중(PAY-003)"),
        ApiResponse(responseCode = "502", description = "빌링키 발급 실패 등 PG 오류(PAY-001)")
    )
    @PostMapping("/subscribe")
    fun subscribe(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: SubscribeRequest
    ): ResponseEntity<Response<SubscriptionResponse>> {
        val response = paymentService.subscribe(principal.id!!, request.planTier, request.customerKey, request.authKey)
        return Response.ok(response).toResponseEntity()
    }
}
