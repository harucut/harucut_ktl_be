package com.harucut.frame.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import org.springframework.stereotype.Component

// 컴포넌트 style 맵 ↔ JSON 문자열 변환 헬퍼
@Component
class FrameStyleConverter(
    private val objectMapper: ObjectMapper
) {
    // style 맵 → JSON 문자열 (비어있으면 "{}")
    fun convertToJson(styleMap: Map<String, Any>?): String {
        if (styleMap.isNullOrEmpty()) return "{}"
        return try {
            objectMapper.writeValueAsString(styleMap)
        } catch (e: Exception) {
            throw BusinessException(GlobalErrorCode.JSON_PARSE_ERROR, "스타일 데이터 변환 중 오류가 발생했습니다.")
        }
    }

    // JSON 문자열 → style 맵 (실패 시 빈 맵)
    fun convertToMap(jsonStyle: String?): Map<String, Any> {
        if (jsonStyle.isNullOrBlank()) return emptyMap()
        return try {
            objectMapper.readValue(jsonStyle, object : TypeReference<Map<String, Any>>() {})
        } catch (e: Exception) {
            emptyMap()
        }
    }
}