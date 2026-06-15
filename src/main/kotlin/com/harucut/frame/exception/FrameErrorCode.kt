package com.harucut.frame.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class FrameErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    FRAME_NOT_FOUND("FRM-001", HttpStatus.NOT_FOUND, "프레임을 찾을 수 없습니다."),
    FRAME_STORAGE_LIMIT_EXCEEDED("FRM-002", HttpStatus.BAD_REQUEST, "프레임 보관 한도를 초과했습니다."),
    FRAME_COMPONENT_NOT_FOUND("FRM-003", HttpStatus.NOT_FOUND, "프레임 컴포넌트를 찾을 수 없습니다.")
}