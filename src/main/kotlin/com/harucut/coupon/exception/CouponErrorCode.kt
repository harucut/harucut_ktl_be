package com.harucut.coupon.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CouponErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    COUPON_NOT_FOUND(
        "COUPON-001",
        HttpStatus.NOT_FOUND,
        "The requested coupon does not exist."
    ),
    COUPON_CODE_DUPLICATED(
        "COUPON-002",
        HttpStatus.CONFLICT,
        "A coupon with this code already exists."
    ),
    INVALID_GRANT_TIER(
        "COUPON-003",
        HttpStatus.BAD_REQUEST,
        "Coupon grant tier must be PLUS or PRO."
    ),
    COUPON_INACTIVE(
        "COUPON-004",
        HttpStatus.BAD_REQUEST,
        "This coupon is inactive or past its redemption deadline."
    ),
    COUPON_EXHAUSTED(
        "COUPON-005",
        HttpStatus.CONFLICT,
        "This coupon has reached its maximum number of redemptions."
    ),
    COUPON_ALREADY_REDEEMED(
        "COUPON-006",
        HttpStatus.CONFLICT,
        "You have already redeemed this coupon."
    ),
    RESERVATION_EXISTS(
        "COUPON-007",
        HttpStatus.CONFLICT,
        "You already have a reserved coupon grant awaiting activation."
    ),
}
