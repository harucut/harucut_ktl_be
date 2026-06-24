package com.harucut.subscription.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class SubscriptionErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    PLAN_VIDEO_UPLOAD_LIMIT_EXCEEDED(
        "SUBS-001",
        HttpStatus.FORBIDDEN,
        "Monthly video upload limit exceeded for the current plan."
    ),
    PLAN_HISTORY_RETENTION_EXCEEDED(
        "SUBS-002",
        HttpStatus.FORBIDDEN,
        "Requested history is beyond the plan's retention period."
    ),
    PLAN_FRAME_RETENTION_EXCEEDED(
        "SUBS-003",
        HttpStatus.FORBIDDEN,
        "The number of stored frames exceeds the limit for the current plan."
    ),
}