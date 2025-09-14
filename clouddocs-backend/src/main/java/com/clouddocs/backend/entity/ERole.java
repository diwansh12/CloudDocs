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
     * ✅ FIXED: Case-insensitive enum deserialization
     * Handles: "manager", "MANAGER", "Manager", "ROLE_MANAGER"
     */
    @JsonCreator
    public static ERole fromString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        String normalizedInput = input.trim().toUpperCase();
        
        // Try direct match first
        for (ERole role : ERole.values()) {
            if (role.name().equals(normalizedInput) || 
                role.getValue().equals(normalizedInput)) {
                return role;
            }
        }
        
        // Try with ROLE_ prefix if missing
        if (!normalizedInput.startsWith("ROLE_")) {
            String withPrefix = "ROLE_" + normalizedInput;
            for (ERole role : ERole.values()) {
                if (role.name().equals(withPrefix)) {
                    return role;
                }
            }
        }
        
        // Log the error and throw descriptive exception
        throw new IllegalArgumentException(
            String.format("Invalid role: '%s'. Valid roles are: %s", 
                         input, java.util.Arrays.toString(ERole.values()))
        );
    }
    
    /**
     * ✅ ENHANCED: Safe enum lookup without exceptions
     */
    public static ERole fromStringSafe(String input) {
        try {
            return fromString(input);
        } catch (IllegalArgumentException e) {
            return null; // or return a default like ROLE_USER
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}
