package com.harucut.config

import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.local.service.CustomUserDetailsService
import com.harucut.auth.security.CustomAuthenticationEntryPoint
import com.harucut.auth.security.filter.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import kotlin.jvm.java

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val customUserDetailsService: CustomUserDetailsService,
    private val jwtTokenService: JwtTokenService,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint
) {

    companion object {
        val PUBLIC_PATHS = arrayOf(
            "/api/harucut/register",
            "/api/harucut/login",
            "/api/harucut/reissue",
            "/api/harucut/logout",
            "/api/harucut/reset/password/code",
            "/api/harucut/reset/password",
            "/api/harucut/reset/password/verification",
            "/api/email-auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
        )
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(http: HttpSecurity): AuthenticationManager {
        val builder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
        builder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder())
        return builder.build()
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .exceptionHandling { it.authenticationEntryPoint(customAuthenticationEntryPoint) }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenService, customUserDetailsService, customAuthenticationEntryPoint),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*PUBLIC_PATHS).permitAll()
                    .anyRequest().authenticated()
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "https://harucut.com",
                "https://www.harucut.com",
                "http://localhost:5173",
                "http://localhost:3000"
            )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            exposedHeaders = listOf("Authorization")
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}