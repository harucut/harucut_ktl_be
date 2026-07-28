package com.harucut.coupon.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.coupon.dto.MyCouponResponse
import com.harucut.coupon.dto.RedeemCouponRequest
import com.harucut.coupon.dto.RedeemResultResponse
import com.harucut.coupon.service.CouponService
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Coupon", description = "내 쿠폰 사용/조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/coupons")
class CouponUserController(
    private val couponService: CouponService
) {

    @Operation(
        summary = "쿠폰 사용",
        description = "코드를 입력해 쿠폰을 사용합니다. 무료 구독 중이 아니면 즉시 개시되고, tier 접근 중이면 현 주기 후로 예약됩니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "사용 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패 또는 비활성/사용마감 쿠폰"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰 코드"),
        ApiResponse(responseCode = "409", description = "사용 상한 도달, 이미 사용함, 또는 이미 예약된 쿠폰 존재")
    )
    @PostMapping("/redeem")
    fun redeem(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: RedeemCouponRequest
    ): ResponseEntity<Response<RedeemResultResponse>> {
        return Response.ok(couponService.redeem(principal.id!!, request.code)).toResponseEntity()
    }

    @Operation(summary = "내 쿠폰 목록 조회", description = "내가 사용한(예약 포함) 쿠폰 목록을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @GetMapping
    fun getMyCoupons(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<List<MyCouponResponse>>> {
        return Response.ok(couponService.getMyCoupons(principal.id!!)).toResponseEntity()
    }
}
