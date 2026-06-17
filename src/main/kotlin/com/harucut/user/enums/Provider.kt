package com.harucut.user.enums

enum class Provider {
    GOOGLE, KAKAO, NAVER, APPLE, HARUCUT;

    companion object {
        fun from(registrationId: String?): Provider {
            requireNotNull(registrationId) { "registrationId는 null일 수 없습니다." }

            return when (registrationId.lowercase()) {
                "harucut" -> HARUCUT
                "google" -> GOOGLE
                "kakao" -> KAKAO
                "naver" -> NAVER
                "apple" -> APPLE
                else -> throw IllegalArgumentException("알 수 없는 OAuth2 제공자: $registrationId")
            }
        }
    }
}