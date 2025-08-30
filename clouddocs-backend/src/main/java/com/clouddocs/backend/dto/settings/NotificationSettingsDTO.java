package com.clouddocs.backend.dto.settings;

import lombok.*;

/**
 * Channels through which the user wishes to receive notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsDTO {

    private boolean emailApproval;   // e-mails for doc approval events
    private boolean sms;             // SMS alerts
    private boolean push;            // Web / mobile push
}
