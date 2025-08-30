package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.settings.GeneralSettingsDTO;
import com.clouddocs.backend.dto.settings.NotificationSettingsDTO;
import com.clouddocs.backend.dto.settings.SecuritySettingsDTO;
import com.clouddocs.backend.dto.settings.UserSettingsDTO;
import com.clouddocs.backend.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for reading and updating the currently-authenticated
 * user’s application, security and notification preferences.
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /* ─────────────────────────────  READ  ───────────────────────────── */

    /**
     * Return the full preference bundle for the current user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSettingsDTO> getSettings() {
        UserSettingsDTO dto = settingsService.getCurrentUserSettings();
        return ResponseEntity.ok(dto);
    }

    /* ─────────────────────────────  UPDATE  ─────────────────────────── */

    /**
     * Update general preferences (app name, timezone, language).
     */
    @PutMapping("/general")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateGeneral(@RequestBody GeneralSettingsDTO dto) {
        log.info("Updating general settings: {}", dto);
        settingsService.updateGeneral(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Update security-related options (2FA, password policy).
     */
    @PutMapping("/security")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateSecurity(@RequestBody SecuritySettingsDTO dto) {
        log.info("Updating security settings: {}", dto);
        settingsService.updateSecurity(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Update notification preferences (email, SMS, push).
     */
    @PutMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateNotifications(@RequestBody NotificationSettingsDTO dto) {
        log.info("Updating notification settings: {}", dto);
        settingsService.updateNotifications(dto);
        return ResponseEntity.ok().build();
    }
}
