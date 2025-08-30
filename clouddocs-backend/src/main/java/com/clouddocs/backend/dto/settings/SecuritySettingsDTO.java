package com.clouddocs.backend.dto.settings;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Security-related user preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettingsDTO {

    private boolean twoFactorEnabled;

    /** Human-readable policy string chosen on the UI. */
    @NotBlank
    private String passwordPolicy;
}
