package com.harucut.exception

import com.harucut.exception.dto.FieldErrorResponse
import com.harucut.util.response.Response
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.util.StringUtils
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 의도적으로 던지는 예외 처리
     * - Service/Domain 레이어에서 throw BusinessException(...) 한 경우
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[BusinessException] code={}, message={}", ex.errorCode.code, ex.message)

        val response = Response.errorResponse<Unit>(ex.errorCode)
        return negotiatedResponse(response, request)
    }

    /**
     * @Valid + @RequestBody 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<List<FieldErrorResponse>>> {
        log.warn("[MethodArgumentNotValidException] message={}", ex.message)

        val errors = ex.bindingResult.fieldErrors.map { error ->
            FieldErrorResponse(
                field = error.field,
                message = error.defaultMessage,
                rejectedValue = error.rejectedValue,
            )
        }

        val response = Response.from(GlobalErrorCode.VALIDATION_FAILED, errors)
        return negotiatedResponse(response, request)
    }

    /**
     * @Valid 없이 바인딩 단계에서 에러가 난 경우 (@ModelAttribute, @RequestParam)
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[BindException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.INVALID_INPUT_VALUE)
        return negotiatedResponse(response, request)
    }

    /**
     * @RequestParam, @PathVariable 타입 변환 실패 시
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[MethodArgumentTypeMismatchException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.TYPE_MISMATCH)
        return negotiatedResponse(response, request)
    }

    /**
     * 필수 RequestParam 누락 시
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[MissingServletRequestParameterException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.MISSING_REQUEST_PARAMETER)
        return negotiatedResponse(response, request)
    }

    /**
     * 지원하지 않는 HTTP 메서드로 요청했을 때
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[HttpRequestMethodNotSupportedException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.METHOD_NOT_ALLOWED)
        return negotiatedResponse(response, request)
    }

    /**
     * 지원하지 않는 Content-Type으로 요청한 경우
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(
        ex: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[HttpMediaTypeNotSupportedException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE)
        return negotiatedResponse(response, request)
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleHttpMediaTypeNotAcceptableException(
        ex: HttpMediaTypeNotAcceptableException,
    ): ResponseEntity<Void> {
        log.warn("[HttpMediaTypeNotAcceptableException] message={}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build()
    }

    /**
     * JSON 파싱 실패, RequestBody를 읽을 수 없는 경우
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[HttpMessageNotReadableException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.JSON_PARSE_ERROR)
        return negotiatedResponse(response, request)
    }

    /**
     * PathVariable / RequestParam에 직접 Bean Validation을 사용했을 때 실패하는 경우
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[ConstraintViolationException] message={}", ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.INVALID_INPUT_VALUE)
        return negotiatedResponse(response, request)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.warn("[NoResourceFoundException] path={}, message={}", ex.resourcePath, ex.message)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.NOT_FOUND)
        return negotiatedResponse(response, request)
    }

    /**
     * 처리되지 않은 모든 예외에 대한 최종 fallback
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<Response<Unit>> {
        log.error("[Exception] unexpected error", ex)

        val response = Response.errorResponse<Unit>(GlobalErrorCode.INTERNAL_SERVER_ERROR)
        return negotiatedResponse(response, request)
    }

    private fun <T> negotiatedResponse(
        response: Response<T>,
        request: HttpServletRequest,
    ): ResponseEntity<Response<T>> {
        return if (acceptsJson(request)) {
            response.toResponseEntity()
        } else {
            ResponseEntity.status(HttpStatus.valueOf(response.status)).build()
        }
    }

    private fun acceptsJson(request: HttpServletRequest): Boolean {
        val acceptHeader = request.getHeader(HttpHeaders.ACCEPT)
        if (!StringUtils.hasText(acceptHeader)) {
            return true
        }

        return try {
            MediaType.parseMediaTypes(acceptHeader).any { mediaType ->
                mediaType.isWildcardType ||
                        mediaType.includes(MediaType.APPLICATION_JSON) ||
                        mediaType.subtype.endsWith("+json")
            }
        } catch (e: IllegalArgumentException) {
            log.warn("[AcceptHeaderParseError] header={}", acceptHeader)
            true
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}