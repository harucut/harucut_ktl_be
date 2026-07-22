package com.harucut.admin.stats.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class AdminErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    INVALID_DATE_RANGE(
        "ADMIN-001",
        HttpStatus.BAD_REQUEST,
        "The requested date range is invalid."
    ),
}
