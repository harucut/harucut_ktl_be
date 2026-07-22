package com.harucut.notice.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class NoticeErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    NOTICE_NOT_FOUND(
        "NOTICE-001",
        HttpStatus.NOT_FOUND,
        "The requested notice does not exist or is not published."
    ),
}
