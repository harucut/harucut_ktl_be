package com.harucut.exception

import com.harucut.util.response.FieldErrorResponse
import com.harucut.util.response.Response
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 비즈니스 로직에서 의도적으로 던지는 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<Response<Unit>> {
        log.warn("[BusinessException] 비즈니스 로직 에러 발생 - 코드: {}, 메시지: {}", ex.errorCode.code, ex.message)
        return Response.errorResponse<Unit>(ex.errorCode).toResponseEntity()
    }

    /**
     * @Valid + @RequestBody 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<Response<List<FieldErrorResponse>>> {
        log.warn("[MethodArgumentNotValidException] DTO 필드 검증 실패 (Bean Validation) - 상세: {}", ex.bindingResult.fieldErrors)

        val errors = ex.bindingResult.fieldErrors.map { error ->
            FieldErrorResponse(
                field = error.field,
                message = error.defaultMessage ?: "Invalid Value",
                rejectedValue = error.rejectedValue
            )
        }

        return Response.from(GlobalErrorCode.VALIDATION_FAILED, errors).toResponseEntity()
    }

    /**
     * 바인딩(값 매핑) 단계에서 에러가 난 경우 (@ModelAttribute, @RequestParam)
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ResponseEntity<Response<Unit>> {
        log.warn("[BindException] 데이터 바인딩 실패 (객체 매핑 오류) - 메시지: {}", ex.message)
        return Response.errorResponse<Unit>(GlobalErrorCode.INVALID_INPUT_VALUE).toResponseEntity()
    }

    /**
     * @RequestParam, @PathVariable 타입 변환 실패 시
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<Response<Unit>> {
        log.warn("[MethodArgumentTypeMismatchException] 파라미터 타입 불일치 - 파라미터명: {}, 요청된 값: {}", ex.name, ex.value)
        return Response.errorResponse<Unit>(GlobalErrorCode.TYPE_MISMATCH).toResponseEntity()
    }

    /**
     * 필수 RequestParam 누락 시
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ResponseEntity<Response<Unit>> {
        log.warn("[MissingServletRequestParameterException] 필수 파라미터 누락 - 파라미터명: {}, 파라미터 타입: {}", ex.parameterName, ex.parameterType)
        return Response.errorResponse<Unit>(GlobalErrorCode.MISSING_PARAMETER).toResponseEntity()
    }

    /**
     * 지원하지 않는 HTTP 메서드로 요청했을 때
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<Response<Unit>> {
        log.warn("[HttpRequestMethodNotSupportedException] 지원하지 않는 HTTP 메서드 요청 - 요청된 메서드: {}, 지원 가능 메서드: {}", ex.method, ex.supportedHttpMethods)
        return Response.errorResponse<Unit>(GlobalErrorCode.METHOD_NOT_ALLOWED).toResponseEntity()
    }

    /**
     * 지원하지 않는 Content-Type으로 요청한 경우
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(ex: HttpMediaTypeNotSupportedException): ResponseEntity<Response<Unit>> {
        log.warn("[HttpMediaTypeNotSupportedException] 지원하지 않는 Content-Type 요청 - 요청된 타입: {}, 지원 가능 타입: {}", ex.contentType, ex.supportedMediaTypes)
        return Response.errorResponse<Unit>(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE).toResponseEntity()
    }

    /**
     * 클라이언트가 수용할 수 없는 Media Type을 요구할 때 (바디 없이 406 상태코드만 반환)
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleHttpMediaTypeNotAcceptableException(ex: HttpMediaTypeNotAcceptableException): ResponseEntity<Void> {
        log.warn("[HttpMediaTypeNotAcceptableException] 수용 불가능한 Media Type (Accept 헤더 불일치) - 메시지: {}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build()
    }

    /**
     * JSON 파싱 실패, RequestBody를 읽을 수 없는 경우
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<Response<Unit>> {
        log.warn("[HttpMessageNotReadableException] JSON 파싱 및 바디 읽기 실패 (형식 오류) - 메시지: {}", ex.message)
        return Response.errorResponse<Unit>(GlobalErrorCode.JSON_PARSE_ERROR).toResponseEntity()
    }

    /**
     * PathVariable / RequestParam 낱개 제약조건 검증 실패 시
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<Response<Unit>> {
        log.warn("[ConstraintViolationException] 제약조건 위반 (파라미터 검증 실패) - 상세: {}", ex.constraintViolations)
        return Response.errorResponse<Unit>(GlobalErrorCode.INVALID_INPUT_VALUE).toResponseEntity()
    }

    /**
     * 잘못된 URL 엔드포인트 요청 시 (404)
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<Response<Unit>> {
        log.warn("[NoResourceFoundException] 요청한 경로를 찾을 수 없음 (404) - 요청 경로: {}", ex.resourcePath)
        return Response.errorResponse<Unit>(GlobalErrorCode.NOT_FOUND).toResponseEntity()
    }

    /**
     * 필수 쿠키(@CookieValue) 누락 시 발생
     */
    @ExceptionHandler(MissingRequestCookieException::class)
    fun handleMissingRequestCookieException(ex: MissingRequestCookieException): ResponseEntity<Response<Unit>> {
        log.warn("[MissingRequestCookieException] 필수 쿠키 누락 - 쿠키명: {}", ex.cookieName)
        return Response.errorResponse<Unit>(GlobalErrorCode.MISSING_PARAMETER).toResponseEntity()
    }

    /**
     * 예상하지 못한 서버 내부 오류 (최종 fallback)
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Response<Unit>> {
        log.error("[Exception] 서버 내부 에러 발생 (예상치 못한 예외)", ex)
        return Response.errorResponse<Unit>(GlobalErrorCode.INTERNAL_SERVER_ERROR).toResponseEntity()
    }
}