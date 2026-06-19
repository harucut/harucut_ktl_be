package com.harucut.config

import com.harucut.auth.oauth2.property.KakaoAuthProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(KakaoAuthProperties::class)
class HttpClientConfig {

    @Bean
    fun restClient(): RestClient = RestClient.create()
}