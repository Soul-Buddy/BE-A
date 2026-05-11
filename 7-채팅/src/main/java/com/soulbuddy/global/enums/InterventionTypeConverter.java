package com.soulbuddy.global.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * InterventionType ↔ DB 컬럼(소문자 snake_case code) 변환기.
 *
 * DB 스키마(schema_0502_v4.sql)의 chat_messages.intervention_type 은
 *   ENUM('sympathy_support','clarification_reflection', ...)
 * 처럼 소문자 code 로 정의되어 있다. enum 상수명(대문자)과 어긋나기 때문에
 * @Enumerated(EnumType.STRING) 으로 매핑하면 SELECT 시
 *   "No enum constant InterventionType.sympathy_support"
 * 가 발생한다. 이 컨버터로 양방향 매핑을 명시적으로 처리한다.
 */
@Converter(autoApply = false)
public class InterventionTypeConverter implements AttributeConverter<InterventionType, String> {

    @Override
    public String convertToDatabaseColumn(InterventionType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public InterventionType convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) return null;
        return InterventionType.fromCode(dbValue);
    }
}
