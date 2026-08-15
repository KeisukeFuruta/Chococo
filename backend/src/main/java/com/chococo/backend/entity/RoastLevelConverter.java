package com.chococo.backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoastLevelConverter implements AttributeConverter<RoastLevel, String> {

    @Override
    public String convertToDatabaseColumn(RoastLevel attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public RoastLevel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RoastLevel.fromDbValue(dbData);
    }
}
