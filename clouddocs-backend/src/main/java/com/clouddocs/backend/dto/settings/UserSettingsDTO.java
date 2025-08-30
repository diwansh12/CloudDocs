package com.clouddocs.backend.dto.settings;

import lombok.*;

/**
 * Aggregated view returned by GET /api/settings.
 * Contains every preference block the Settings screen needs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsDTO {

    private GeneralSettingsDTO general;
    private SecuritySettingsDTO security;
    private NotificationSettingsDTO notifications;
}
