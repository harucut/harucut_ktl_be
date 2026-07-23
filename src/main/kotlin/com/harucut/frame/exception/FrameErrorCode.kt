package com.harucut.frame.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class FrameErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    SYSTEM_FRAME_NOT_FOUND(
        "FRAME-001",
        HttpStatus.NOT_FOUND,
        "The requested system frame does not exist."
    ),
}
