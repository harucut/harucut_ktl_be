package com.harucut.media.exception

import com.harucut.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class MediaErrorCode(
    override val code: String,
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    MEDIA_NOT_FOUND("MED-001", HttpStatus.NOT_FOUND, "미디어를 찾을 수 없습니다."),
    DUPLICATE_S3_KEY("MED-002", HttpStatus.CONFLICT, "이미 존재하는 S3 키입니다."),
    TRANSCODE_TASK_NOT_FOUND("MED-003", HttpStatus.NOT_FOUND, "변환 작업을 찾을 수 없습니다.")
}
