package com.harucut.subscription.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class SubscriptionErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    SUBSCRIPTION_NOT_FOUND("SUB-001", HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    PAYMENT_ALREADY_PROCESSED("SUB-002", HttpStatus.CONFLICT, "이미 처리된 결제 이벤트입니다."),
    TRANSCODE_LIMIT_EXCEEDED("SUB-003", HttpStatus.BAD_REQUEST, "이번 달 동영상 변환 횟수를 초과했습니다.")
}