package com.clouddocs.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ERole {
    ROLE_USER("USER"),
    ROLE_ADMIN("ADMIN"), 
    ROLE_MANAGER("MANAGER");
    
    private final String value;
    
    ERole(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    /**
     * ✅ CRITICAL FIX: Handle both "MANAGER" and "ROLE_MANAGER" formats
     */
    @JsonCreator
    public static ERole fromString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        String normalizedInput = input.trim().toUpperCase();
        
        // ✅ Handle direct value matches ("MANAGER" -> ROLE_MANAGER)
        switch (normalizedInput) {
            case "USER": return ROLE_USER;
            case "ADMIN": return ROLE_ADMIN;
            case "MANAGER": return ROLE_MANAGER;
            case "ROLE_USER": return ROLE_USER;
            case "ROLE_ADMIN": return ROLE_ADMIN;
            case "ROLE_MANAGER": return ROLE_MANAGER;
            default:
                throw new IllegalArgumentException(
                    String.format("Invalid role: '%s'. Valid roles are: USER, ADMIN, MANAGER, ROLE_USER, ROLE_ADMIN, ROLE_MANAGER", input)
                );
        }
    }
    
    /**
     * ✅ SAFE: No-exception version
     */
    public static ERole fromStringSafe(String input) {
        try {
            return fromString(input);
        } catch (Exception e) {
            return ROLE_USER; // Default fallback
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}
