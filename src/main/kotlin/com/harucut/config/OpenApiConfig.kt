package com.harucut.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    companion object {
        private const val BEARER_SCHEME = "bearerAuth"
    }

    @Bean
    fun openAPI(): OpenAPI {
        val bearer = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .name(BEARER_SCHEME)

        // 전역 보안 요건을 걸지 않는다(공개 엔드포인트에 불필요한 자물쇠 방지).
        // 인증이 필요한 엔드포인트에만 @SecurityRequirement(name = "bearerAuth") 를 붙인다.
        return OpenAPI()
            .info(apiInfo())
            .servers(
                listOf(
                    Server().url("https://api.harucut.com").description("배포 서버"),
                    Server().url("http://localhost:8080").description("로컬 개발")
                )
            )
            .components(Components().addSecuritySchemes(BEARER_SCHEME, bearer))
    }

    private fun apiInfo(): Info =
        Info()
            .title("Harucut API")
            .version("v1")
            .description(
                """
                하루컷 백엔드 API 문서.

                ## 인증 테스트 방법
                운영 인증은 **httpOnly `accessToken` 쿠키**가 1차 경로입니다. Swagger UI에서는 쿠키를 직접 다룰 수 없으므로,
                테스트 편의를 위해 동일 토큰을 `Authorization: Bearer` 헤더로도 허용합니다.

                1. `POST /api/harucut/login` 을 실행합니다.
                2. 응답 헤더의 `Set-Cookie: accessToken=<JWT>` 에서 JWT 값을 복사합니다.
                3. 우측 상단 **Authorize** 버튼 → `bearerAuth` 에 복사한 JWT를 붙여넣습니다.
                4. 이후 인증이 필요한 API를 그대로 호출할 수 있습니다.

                ## 소셜 로그인
                `/oauth2/authorization/{google|kakao|naver}` 는 브라우저 리다이렉트 플로우라 Swagger의 "Try it out"으로 테스트할 수 없습니다.
                브라우저에서 직접 접속해 로그인한 뒤 발급된 토큰으로 위 Authorize를 사용하세요.
                """.trimIndent()
            )
}
