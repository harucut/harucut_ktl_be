package com.harucut.user.enums

enum class PlanTier(
    val monthlyTranscodeLimit: Int,   // 월 동영상 변환 횟수. -1 = 무제한
    val frameStorageLimit: Int,       // 보관 가능한 프레임 수.  -1 = 무제한
    val historyRetentionDays: Int     // 미디어 이력 보관 일수. -1 = 무제한
) {
    BASIC(5, 1, 3),
    PLUS(30, 5, -1),
    PRO(-1, 10, -1);

    val isTranscodeUnlimited: Boolean get() = monthlyTranscodeLimit < 0
    val isFrameStorageUnlimited: Boolean get() = frameStorageLimit < 0
    val isHistoryUnlimited: Boolean get() = historyRetentionDays < 0
}