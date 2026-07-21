package com.harucut.terms.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class TermsErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    TERMS_NOT_FOUND(
        "TERMS-001",
        HttpStatus.NOT_FOUND,
        "The requested terms do not exist or are inactive."
    ),
    TERMS_CODE_DUPLICATED(
        "TERMS-002",
        HttpStatus.CONFLICT,
        "Terms code already exists."
    ),
    REQUIRED_TERMS_CANNOT_WITHDRAW(
        "TERMS-003",
        HttpStatus.BAD_REQUEST,
        "Required terms cannot be withdrawn."
    ),
}
