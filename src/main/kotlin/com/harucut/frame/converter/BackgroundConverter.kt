package com.harucut.frame.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.attributes.BackgroundAttributes
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class BackgroundConverter : AttributeConverter<BackgroundAttributes, String> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    // 엔티티 → DB: 배경 속성을 JSON 문자열로 직렬화
    override fun convertToDatabaseColumn(attribute: BackgroundAttributes?): String? {
        if (attribute == null) return null
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            throw BusinessException(GlobalErrorCode.JSON_PARSE_ERROR)
        }
    }

    // DB → 엔티티: JSON 문자열을 배경 속성으로 역직렬화
    override fun convertToEntityAttribute(dbData: String?): BackgroundAttributes? {
        if (dbData.isNullOrBlank()) return null
        return try {
            objectMapper.readValue(dbData)
        } catch (e: Exception) {
            throw BusinessException(GlobalErrorCode.JSON_PARSE_ERROR)
        }
    }
}