package com.harucut.frame.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.entity.attributes.BackgroundAttributes
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class BackgroundConverter : AttributeConverter<BackgroundAttributes, String> {

    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: BackgroundAttributes): String =
        runCatching { objectMapper.writeValueAsString(attribute) }
            .getOrElse { throw BusinessException(GlobalErrorCode.JSON_PARSE_ERROR) }

    override fun convertToEntityAttribute(dbData: String): BackgroundAttributes =
        runCatching { objectMapper.readValue(dbData, BackgroundAttributes::class.java) }
            .getOrElse { throw BusinessException(GlobalErrorCode.JSON_PARSE_ERROR) }
}