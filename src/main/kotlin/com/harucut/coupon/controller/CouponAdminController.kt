package com.harucut.coupon.controller

import com.harucut.coupon.dto.CouponResponse
import com.harucut.coupon.dto.CreateCouponRequest
import com.harucut.coupon.service.CouponAdminService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Coupon Admin", description = "관리자 쿠폰 CRUD API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/coupons")
class CouponAdminController(
    private val couponAdminService: CouponAdminService
) {

    @Operation(summary = "쿠폰 생성", description = "코드 기반 무료 쿠폰을 생성합니다. 부여 tier는 PLUS/PRO만 허용됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패 또는 BASIC tier 지정"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "409", description = "이미 존재하는 쿠폰 코드")
    )
    @PostMapping
    fun createCoupon(@RequestBody @Valid request: CreateCouponRequest): ResponseEntity<Response<Unit>> {
        couponAdminService.createCoupon(request.name, request.code, request.grantTier, request.maxRedemptions, request.validUntil)
        return Response.ok().toResponseEntity()
    }

    @Operation(summary = "전체 쿠폰 목록 조회", description = "전체 쿠폰 목록을 누적 사용 수와 함께 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    )
    @GetMapping
    fun getAllCoupons(): ResponseEntity<Response<List<CouponResponse>>> {
        return Response.ok(couponAdminService.listCoupons()).toResponseEntity()
    }

    @Operation(summary = "쿠폰 비활성화", description = "쿠폰을 비활성화합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "비활성화 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰")
    )
    @PatchMapping("/{publicId}/deactivate")
    fun deactivateCoupon(
        @Parameter(description = "쿠폰 공개 ID", required = true) @PathVariable publicId: String
    ): ResponseEntity<Response<Unit>> {
        couponAdminService.deactivateCoupon(publicId)
        return Response.ok().toResponseEntity()
    }
}
