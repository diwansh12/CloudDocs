package com.clouddocs.backend.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Application-wide preferences that are not security–sensitive.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralSettingsDTO {

    @NotBlank
    @Size(max = 80)
    private String appName;          // e.g. “CloudDocs”

    @NotBlank
    private String timezone;         // IANA ID, e.g. “UTC”, “Europe/Berlin”

    @NotBlank
    private String language;         // ISO-639 code or friendly name
}
