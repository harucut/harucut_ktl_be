package com.harucut.exception

import org.springframework.http.HttpStatus

enum class GlobalErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    // 400 Bad Request
    BAD_REQUEST("GEN-001", HttpStatus.BAD_REQUEST, "Bad Request"),
    INVALID_INPUT_VALUE("GEN-002", HttpStatus.BAD_REQUEST, "Invalid Input Value"),
    VALIDATION_FAILED("GEN-003", HttpStatus.BAD_REQUEST, "Validation Failed"),
    MISSING_REQUEST_PARAMETER("GEN-004", HttpStatus.BAD_REQUEST, "Missing Request Parameter"),
    TYPE_MISMATCH("GEN-005", HttpStatus.BAD_REQUEST, "Type Mismatch"),
    JSON_PARSE_ERROR("GEN-006", HttpStatus.BAD_REQUEST, "Json Parse Error"),
    DUPLICATE_REQUEST("GEN-007", HttpStatus.BAD_REQUEST, "Duplicate Request"),
    FILE_EXPIRED("GEN-008", HttpStatus.BAD_REQUEST, "File Expired"),

    // 401 Unauthorized
    UNAUTHORIZED("GEN-009", HttpStatus.UNAUTHORIZED, "Unauthorized"),

    // 403 Forbidden
    FORBIDDEN("GEN-010", HttpStatus.FORBIDDEN, "Forbidden"),

    // 404 Not Found
    NOT_FOUND("GEN-011", HttpStatus.NOT_FOUND, "Resource Not Found"),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED("GEN-012", HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed"),

    // 415 Unsupported Media Type
    UNSUPPORTED_MEDIA_TYPE("GEN-013", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR("GEN-099", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    DATABASE_ERROR("GEN-100", HttpStatus.INTERNAL_SERVER_ERROR, "Database Error"),
    IO_ERROR("GEN-101", HttpStatus.INTERNAL_SERVER_ERROR, "IO Error"),
    REDIS_CONNECTION_ERROR("GEN-102", HttpStatus.INTERNAL_SERVER_ERROR, "Redis Connection Error")
}