package com.harucut.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock

@Configuration
@EnableScheduling
class SchedulingConfig {

    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}