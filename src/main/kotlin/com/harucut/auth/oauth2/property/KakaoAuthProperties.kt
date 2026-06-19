package com.harucut.auth.oauth2.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kakao.auth")
data class KakaoAuthProperties(
    val adminKey: String,
    val unlinkUrl: String,
    val unlinkContentType: String,
    val unlinkTargetIdType: String
)
