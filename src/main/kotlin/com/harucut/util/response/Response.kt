package com.harucut.util.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Response<T>(
    val code: String? = null,
    val status: Int = 0,
    val message: String? = null,
    val data: T? = null,
) {
    fun toResponseEntity(): ResponseEntity<Response<T>> =
        ResponseEntity(this, HttpStatus.valueOf(status))

    // TODO(auth/jwt 단계): AuthTokenCookies 마이그레이션 후 아래 오버로드 추가
    // fun toResponseEntity(cookies: AuthTokenCookies): ResponseEntity<Response<T>> = ...

    companion object {
        private const val OK_CODE = "GEN-000"

        fun ok(): Response<Unit> =
            Response(code = OK_CODE, status = HttpStatus.OK.value())

        fun <T> ok(data: T): Response<T> =
            Response(code = OK_CODE, status = HttpStatus.OK.value(), data = data)

        fun <T> errorResponse(errorCode: ErrorCode): Response<T> =
            Response(
                code = errorCode.code,
                status = errorCode.httpStatus.value(),
                message = errorCode.message,
            )

        fun <T> from(errorCode: ErrorCode, data: T): Response<T> =
            Response(
                code = errorCode.code,
                status = errorCode.httpStatus.value(),
                message = errorCode.message,
                data = data,
            )
    }
}
