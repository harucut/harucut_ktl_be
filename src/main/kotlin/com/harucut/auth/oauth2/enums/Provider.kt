package com.harucut.auth.oauth2.enums

enum class Provider {
    GOOGLE, KAKAO, NAVER, HARUCUT;

    companion object {
        fun from(registrationId: String?): Provider {
            requireNotNull(registrationId) { "registrationId is null" }
            return when (registrationId.lowercase()) {
                "harucut" -> HARUCUT
                "google" -> GOOGLE
                "kakao" -> KAKAO
                "naver" -> NAVER
                else -> throw IllegalArgumentException("Unknown Provider: $registrationId")
            }
        }
    }
}