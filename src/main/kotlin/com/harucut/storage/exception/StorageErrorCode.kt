package com.harucut.storage.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class StorageErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    UNSUPPORTED_UPLOAD_TYPE("STOR-000", HttpStatus.BAD_REQUEST, "Unsupported upload type.")
}