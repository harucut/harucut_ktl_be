package com.harucut.config

import com.harucut.auth.jwt.property.JwtProperties
import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.jwt.service.JwtTokenServiceImpl
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {

    @Bean
    fun jwtTokenService(properties: JwtProperties): JwtTokenService =
        JwtTokenServiceImpl(
            secretKey = properties.secret,
            accessTokenValidityMillis = properties.accessExpiration.toMillis(),
            refreshTokenValidityMillis = properties.refreshExpiration.toMillis()
        )
}