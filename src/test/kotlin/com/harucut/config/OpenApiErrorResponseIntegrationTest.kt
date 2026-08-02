package com.harucut.config

import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

// 실제 /v3/api-docs 산출물로 GET /api/admin/frames 의 에러 응답 스키마가
// 성공 스키마 재사용에서 벗어나 ErrorResponse 를 가리키는지 확인하는 통합 테스트.
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("에러 응답 스키마 - 실제 /v3/api-docs 산출물 검증")
class OpenApiErrorResponseIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("GET /api/admin/frames 의 200은 성공 스키마를, 401/403은 ErrorResponse 스키마를 가리킨다")
    fun frameAdminListSchemas() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$['paths']['/api/admin/frames']['get']['responses']['200']['content']['application/json']['schema']['\$ref']") {
                value("#/components/schemas/ResponseListFrameResponse")
            }
            jsonPath("$['paths']['/api/admin/frames']['get']['responses']['401']['content']['application/json']['schema']['\$ref']") {
                value("#/components/schemas/ErrorResponse")
            }
            jsonPath("$['paths']['/api/admin/frames']['get']['responses']['403']['content']['application/json']['schema']['\$ref']") {
                value("#/components/schemas/ErrorResponse")
            }
        }
    }

    @Test
    @DisplayName("ErrorResponse 컴포넌트 스키마의 property description/example 이 유실되지 않는다")
    fun errorResponsePropertySchemaIsPreserved() {
        // Kotlin 생성자 파라미터에 use-site target 없이 @Schema를 붙이면 PARAMETER 타깃으로만 적용되어
        // swagger-core가 프로퍼티 스키마(description/example)를 읽지 못하는 회귀가 있었다(ErrorResponseSchema.kt 참고).
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$['components']['schemas']['ErrorResponse']['properties']['code']['description']") {
                value("에러 코드")
            }
            jsonPath("$['components']['schemas']['ErrorResponse']['properties']['code']['example']") {
                value("AUTH-010")
            }
            jsonPath("$['components']['schemas']['ErrorResponse']['properties']['status']['description']") {
                value("HTTP 상태 코드")
            }
            jsonPath("$['components']['schemas']['ErrorResponse']['properties']['message']['description']") {
                value("에러 메시지")
            }
            jsonPath("$['components']['schemas']['ValidationErrorResponse']['properties']['data']['description']") {
                value("필드별 검증 실패 상세")
            }
        }
    }

    @Test
    @DisplayName("FieldErrorResponse 컴포넌트 스키마의 property description/example 이 유실되지 않고, 자체 description도 오염되지 않는다")
    fun fieldErrorResponsePropertySchemaIsPreserved() {
        // FieldErrorResponse는 실제 런타임 응답(@Valid 400)에 실려 나가는 타입이라 손댈 확률이 가장 높다.
        // 여기에 use-site target 없는 평범한 @Schema로 필드를 추가하면 description/example이 조용히 유실된다.
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$['components']['schemas']['FieldErrorResponse']['description']") {
                value("필드 검증 실패 상세")
            }
            jsonPath("$['components']['schemas']['FieldErrorResponse']['properties']['field']['description']") {
                value("검증 실패한 필드명")
            }
            jsonPath("$['components']['schemas']['FieldErrorResponse']['properties']['field']['example']") {
                value("email")
            }
        }
    }
}
