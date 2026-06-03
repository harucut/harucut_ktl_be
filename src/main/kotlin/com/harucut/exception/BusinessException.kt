package com.harucut.exception

class BusinessException(
    val errorCode: ErrorCode,
    customMessage: String? = null
) : RuntimeException(customMessage ?: errorCode.message)