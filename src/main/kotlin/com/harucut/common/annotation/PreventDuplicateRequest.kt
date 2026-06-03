package com.harucut.common.annotation

import com.harucut.common.enums.LockStrategy
import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PreventDuplicateRequest(
    val key: String = "",
    val time: Long = 3000,
    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    val strategy: LockStrategy = LockStrategy.FAIL_CLOSE,
)
