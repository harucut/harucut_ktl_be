package com.harucut.support

import com.harucut.auth.jwt.service.JwtTokenService
import com.harucut.auth.oauth2.service.CustomOAuth2UserService
import com.harucut.auth.oauth2.service.CustomOidcUserService
import com.harucut.auth.security.CustomAuthenticationEntryPoint
import com.harucut.auth.security.handler.CustomOAuth2FailureHandler
import com.harucut.auth.security.handler.CustomOAuth2SuccessHandler
import com.harucut.auth.security.service.CustomUserDetailsService
import com.ninjasquad.springmockk.MockkBean

/**
 * `@WebMvcTest` + `@Import(SecurityConfig)` 컨트롤러 테스트의 공통 SecurityConfig 의존성 모킹.
 *
 * SecurityConfig 생성자가 요구하는 빈들을 한곳에서 @MockkBean 으로 제공한다.
 * 컨트롤러 테스트는 이 클래스를 상속하고, 컨트롤러 전용 서비스만 추가로 @MockkBean 하면 된다.
 */
abstract class SecurityBeansMockSupport {

    @MockkBean
    lateinit var customUserDetailsService: CustomUserDetailsService

    @MockkBean
    lateinit var jwtTokenService: JwtTokenService

    @MockkBean
    lateinit var customAuthenticationEntryPoint: CustomAuthenticationEntryPoint

    @MockkBean
    lateinit var customOAuth2UserService: CustomOAuth2UserService

    @MockkBean
    lateinit var customOidcUserService: CustomOidcUserService

    @MockkBean
    lateinit var customOAuth2SuccessHandler: CustomOAuth2SuccessHandler

    @MockkBean
    lateinit var customOAuth2FailureHandler: CustomOAuth2FailureHandler
}
