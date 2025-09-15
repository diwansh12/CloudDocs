package com.clouddocs.backend.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.clouddocs.backend.entity.ERole;

@Converter(autoApply = true)
public class ERoleConverter implements AttributeConverter<ERole, String> {

    @Override
    public String convertToDatabaseColumn(ERole role) {
        if (role == null) return null;
        // Store short form without prefix in DB
        return role.getValue(); // "MANAGER", "ADMIN", "USER"
    }

    @Override
    public ERole convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // Map DB string to enum constant using your custom fromString
        return ERole.fromString(dbData);
    }
}
