package com.harucut.util.response

data class FieldErrorResponse(
    val field: String,
    val message: String?,
    val rejectedValue: Any?
)