package com.harucut.auth.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {

    // 401 Unauthorized
    AUTHENTICATION_FAILED("AUTH-000", HttpStatus.UNAUTHORIZED, "Authentication failed."),
    INVALID_TOKEN("AUTH-008", HttpStatus.UNAUTHORIZED, "Invalid access token."),
    EXPIRED_TOKEN("AUTH-009", HttpStatus.UNAUTHORIZED, "Expired access token."),

    // 400 Bad Request
    INVALID_CREDENTIALS("AUTH-001", HttpStatus.BAD_REQUEST, "Invalid credentials."),
    WRONG_PASSWORD("AUTH-003", HttpStatus.BAD_REQUEST, "Incorrect password."),
    DELETED_REQUEST_USER("AUTH-005", HttpStatus.BAD_REQUEST, "This account is pending deletion."),
    DELETED_USER("AUTH-006", HttpStatus.BAD_REQUEST, "This account has been permanently deleted."),
    EMAIL_AUTH_FAILED("AUTH-011", HttpStatus.BAD_REQUEST, "Invalid or expired verification code."),
    REGISTER_EMAIL_ERROR("AUTH-012", HttpStatus.BAD_REQUEST, "Failed to register email."),

    // 404 Not Found
    NOT_EXIST_USER("AUTH-002", HttpStatus.NOT_FOUND, "User not found."),

    // 409 Conflict
    EMAIL_DUPLICATED("AUTH-004", HttpStatus.CONFLICT, "This email is already in use."),

    // 500 Internal Server Error
    OAUTH2_PROVIDER_UNLINK_ERROR(
        "AUTH-007",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to unlink OAuth2 provider account."
    ),
    EMAIL_SEND_FAILED("AUTH-010", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send verification email.")
}