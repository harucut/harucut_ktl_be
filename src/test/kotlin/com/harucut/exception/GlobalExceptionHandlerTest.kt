package com.harucut.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test

class GlobalExceptionHandlerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())

        mockMvc = MockMvcBuilders
            .standaloneSetup(FakeController())
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()
    }

    @Test
    @DisplayName("BusinessException → 에러코드에 맞는 status·code·message 반환")
    fun businessException() {
        mockMvc.perform(get("/test/business"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("GEN-007"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("리소스가 존재하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("BusinessException(customMessage) → customMessage가 message 필드에 노출됨")
    fun businessExceptionCustomMessage() {
        mockMvc.perform(get("/test/business-custom"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GEN-011"))
            .andExpect(jsonPath("$.message").value("권한이 없는 요청입니다."))
    }

    @Test
    @DisplayName("@Valid 검증 실패 → 400 VALIDATION_FAILED + 필드 에러 목록")
    fun validationFailed() {
        mockMvc.perform(
            post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": ""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GEN-003"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].field").value("name"))
    }

    @Test
    @DisplayName("파라미터 타입 불일치 → 400 TYPE_MISMATCH")
    fun typeMismatch() {
        mockMvc.perform(get("/test/typed?id=notANumber"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GEN-005"))
    }

    @Test
    @DisplayName("필수 파라미터 누락 → 400 MISSING_REQUEST_PARAMETER")
    fun missingRequestParameter() {
        mockMvc.perform(get("/test/typed"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GEN-004"))
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 → 405 METHOD_NOT_ALLOWED")
    fun methodNotAllowed() {
        mockMvc.perform(post("/test/business"))   // GET only endpoint에 POST 요청
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.code").value("GEN-012"))
    }

    @Test
    @DisplayName("잘못된 JSON → 400 JSON_PARSE_ERROR")
    fun jsonParseError() {
        mockMvc.perform(
            post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GEN-006"))
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type → 415 UNSUPPORTED_MEDIA_TYPE")
    fun unsupportedMediaType() {
        mockMvc.perform(
            post("/test/validation")
                .contentType(MediaType.TEXT_PLAIN)
                .content("some text"),
        )
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("GEN-009"))
    }

    @Test
    @DisplayName("예상치 못한 예외 → 500 INTERNAL_SERVER_ERROR")
    fun internalServerError() {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("GEN-099"))
    }

    @RestController
    @Validated
    class FakeController {
        @GetMapping("/test/business")
        fun throwBusiness(): String =
            throw BusinessException(GlobalErrorCode.NOT_FOUND)

        @GetMapping("/test/business-custom")
        fun throwBusinessCustom(): String =
            throw BusinessException(GlobalErrorCode.FORBIDDEN, "권한이 없습니다.")

        @PostMapping("/test/validation")
        fun throwValidation(@Valid @RequestBody dto: ValidDto): String = "ok"

        @GetMapping("/test/typed")
        fun throwTyped(@RequestParam("id") id: Long): String = id.toString()

        @GetMapping("/test/unexpected")
        fun throwUnexpected(): String = throw RuntimeException("unexpected")
    }

    data class ValidDto(
        @field:NotBlank
        val name: String?,
    )
}