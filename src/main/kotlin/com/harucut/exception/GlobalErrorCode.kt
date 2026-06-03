package com.harucut.exception

import org.springframework.http.HttpStatus

enum class GlobalErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    // 400
    BAD_REQUEST("GEN-001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE("GEN-002", HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    VALIDATION_FAILED("GEN-003", HttpStatus.BAD_REQUEST, "검증에 실패했습니다."),
    MISSING_REQUEST_PARAMETER("GEN-004", HttpStatus.BAD_REQUEST, "요청 파라미터가 없거나 부족합니다."),
    TYPE_MISMATCH("GEN-005", HttpStatus.BAD_REQUEST, "타입 에러가 발생했습니다."),
    JSON_PARSE_ERROR("GEN-006", HttpStatus.BAD_REQUEST, "Json 형식이 맞지 않습니다."),
    DUPLICATE_REQUEST("GEN-007", HttpStatus.BAD_REQUEST, "너무 잦은 요청입니다. 잠시 후 다시 시도해주세요."),
    FILE_EXPIRED("GEN-103", HttpStatus.BAD_REQUEST, "만료된 파일입니다."),

    // 404
    NOT_FOUND("GEN-007", HttpStatus.NOT_FOUND, "리소스가 존재하지 않습니다."),

    // 405
    METHOD_NOT_ALLOWED("GEN-012", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),

    // 415
    UNSUPPORTED_MEDIA_TYPE("GEN-009", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Media Type 입니다."),

    // 401/403
    UNAUTHORIZED("GEN-010", HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),
    FORBIDDEN("GEN-011", HttpStatus.FORBIDDEN, "권한이 없는 요청입니다."),

    // 500
    INTERNAL_SERVER_ERROR("GEN-099", HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다."),
    DATABASE_ERROR("GEN-100", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 에러가 발생했습니다."),
    IO_ERROR("GEN-101", HttpStatus.INTERNAL_SERVER_ERROR, "IO 에러가 발생했습니다."),
    REDIS_CONNECTION_ERROR("GEN-102", HttpStatus.INTERNAL_SERVER_ERROR, "레디스 연결 중 에러가 발생했습니다."),
}