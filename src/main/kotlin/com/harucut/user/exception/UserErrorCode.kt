package com.harucut.user.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    USER_NOT_FOUND("USR-001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_USER("USR-002", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다.")
}