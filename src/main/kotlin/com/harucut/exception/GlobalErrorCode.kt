package com.harucut.exception

import org.springframework.http.HttpStatus

enum class GlobalErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    // 400 Bad Request (GEN-000 대역)
    BAD_REQUEST("GEN-001", HttpStatus.BAD_REQUEST, "Bad request."),
    INVALID_INPUT_VALUE("GEN-002", HttpStatus.BAD_REQUEST, "Invalid input value."),
    VALIDATION_FAILED("GEN-003", HttpStatus.BAD_REQUEST, "Validation failed."),
    MISSING_PARAMETER("GEN-004", HttpStatus.BAD_REQUEST, "Missing request parameter."),
    TYPE_MISMATCH("GEN-005", HttpStatus.BAD_REQUEST, "Type mismatch error."),
    JSON_PARSE_ERROR("GEN-006", HttpStatus.BAD_REQUEST, "Failed to parse JSON body."),
    DUPLICATE_REQUEST("GEN-007", HttpStatus.BAD_REQUEST, "Duplicate request."),
    FILE_EXPIRED("GEN-008", HttpStatus.BAD_REQUEST, "The requested file has expired."),

    // 401 Unauthorized (GEN-010 대역)
    UNAUTHORIZED("GEN-011", HttpStatus.UNAUTHORIZED, "Unauthorized access."),

    // 403 Forbidden (GEN-020 대역)
    FORBIDDEN("GEN-021", HttpStatus.FORBIDDEN, "Access denied."),

    // 404 Not Found (GEN-030 대역)
    NOT_FOUND("GEN-031", HttpStatus.NOT_FOUND, "Resource not found."),

    // 405 Method Not Allowed (GEN-040 대역)
    METHOD_NOT_ALLOWED("GEN-041", HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed."),

    // 415 Unsupported Media Type (GEN-050 대역)
    UNSUPPORTED_MEDIA_TYPE("GEN-051", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type."),

    // 500 Internal Server Error (GEN-090 대역)
    INTERNAL_SERVER_ERROR("GEN-091", HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred."),
    DATABASE_ERROR("GEN-092", HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred."),
    IO_ERROR("GEN-093", HttpStatus.INTERNAL_SERVER_ERROR, "Input/Output error occurred."),
    REDIS_CONNECTION_ERROR("GEN-094", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect to Redis server.")
}