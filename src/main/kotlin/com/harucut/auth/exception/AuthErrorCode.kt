package com.harucut.auth.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    // 400 Bad Request
    INVALID_CREDENTIALS("AUTH-001", HttpStatus.BAD_REQUEST, "Invalid credentials."),
    WRONG_PASSWORD("AUTH-002", HttpStatus.BAD_REQUEST, "Incorrect password."),
    EMAIL_VERIFICATION_FAILED("AUTH-003", HttpStatus.BAD_REQUEST, "Invalid or expired verification code."),
    EMAIL_REGISTRATION_FAILED("AUTH-004", HttpStatus.BAD_REQUEST, "Failed to register email."),
    DELETED_REQUEST_USER("AUTH-005", HttpStatus.BAD_REQUEST, "This account is pending deletion."),
    DELETED_USER("AUTH-006", HttpStatus.BAD_REQUEST, "This account has been permanently deleted."),

    // 401 Unauthorized
    AUTHENTICATION_FAILED("AUTH-010", HttpStatus.UNAUTHORIZED, "Authentication failed."),
    INVALID_TOKEN("AUTH-011", HttpStatus.UNAUTHORIZED, "Invalid access token."),
    EXPIRED_TOKEN("AUTH-012", HttpStatus.UNAUTHORIZED, "Expired access token."),

    // 404 Not Found
    USER_NOT_FOUND("AUTH-020", HttpStatus.NOT_FOUND, "User not found."),

    // 409 Conflict
    EMAIL_ALREADY_EXISTS("AUTH-030", HttpStatus.CONFLICT, "This email is already in use."),

    // 500 Internal Server Error
    EMAIL_SEND_FAILED("AUTH-090", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send verification email."),
    OAUTH2_UNLINK_FAILED("AUTH-091", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to unlink OAuth2 provider account.")
}