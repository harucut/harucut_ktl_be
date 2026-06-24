package com.harucut.storage.util

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import java.net.URI

// 입력(s3://, http(s)://, key)을 순수 S3 object key로 정규화. 비어있거나 key를 추출할 수 없으면 예외
fun normalizeToS3Key(pathOrKey: String?): String {
    if (pathOrKey.isNullOrBlank()) {
        throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "S3 path or key must not be blank.")
    }
    val value = pathOrKey.trim()
    if (value.startsWith("s3://") || value.startsWith("http://") || value.startsWith("https://")) {
        val key = URI.create(value).path
        if (key.isNullOrBlank() || key == "/") {
            throw BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "Cannot extract S3 key from the given path.")
        }
        return if (key.startsWith("/")) key.substring(1) else key
    }
    return if (value.startsWith("/")) value.substring(1) else value
}
