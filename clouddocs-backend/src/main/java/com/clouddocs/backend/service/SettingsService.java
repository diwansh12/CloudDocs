package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.settings.GeneralSettingsDTO;
import com.clouddocs.backend.dto.settings.NotificationSettingsDTO;
import com.clouddocs.backend.dto.settings.SecuritySettingsDTO;
import com.clouddocs.backend.dto.settings.UserSettingsDTO;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.UserSettings;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.UserSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates business-logic for reading and persisting a user’s
 * application, security and notification preferences.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;

    /* ───────────────────────────  PUBLIC  ─────────────────────────── */

    public UserSettingsDTO getCurrentUserSettings() {
        User user = currentUser();
        UserSettings settings = settingsRepository
                .findByUserId(user.getId())
                .orElseGet(() -> createDefaultsFor(user));

        return mapToDTO(settings);
    }

    public void updateGeneral(GeneralSettingsDTO dto) {
        UserSettings s = obtainSettingsForCurrentUser();
        s.setAppName(dto.getAppName());
        s.setTimezone(dto.getTimezone());
        s.setLanguage(dto.getLanguage());
        settingsRepository.save(s);
        log.info("General settings updated for user {}", s.getUser().getUsername());
    }

    public void updateSecurity(SecuritySettingsDTO dto) {
        UserSettings s = obtainSettingsForCurrentUser();
        s.setTwoFactorEnabled(dto.isTwoFactorEnabled());
        s.setPasswordPolicy(dto.getPasswordPolicy());
        settingsRepository.save(s);
        log.info("Security settings updated for user {}", s.getUser().getUsername());
    }

    public void updateNotifications(NotificationSettingsDTO dto) {
        UserSettings s = obtainSettingsForCurrentUser();
        s.setEmailApproval(dto.isEmailApproval());
        s.setSms(dto.isSms());
        s.setPush(dto.isPush());
        settingsRepository.save(s);
        log.info("Notification settings updated for user {}", s.getUser().getUsername());
    }

    /* ───────────────────────  INTERNAL HELPERS  ───────────────────── */

    private User currentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    private UserSettings obtainSettingsForCurrentUser() {
        User user = currentUser();
        return settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultsFor(user));
    }

    private UserSettings createDefaultsFor(User user) {
        UserSettings defaults = UserSettings.builder()
                .user(user)
                .appName("CloudDocs")
                .timezone("UTC")
                .language("English")
                .twoFactorEnabled(false)
                .passwordPolicy(
                        "Strong (Min 8 chars, mixed case, number, symbol)")
                .emailApproval(true)
                .sms(false)
                .push(true)
                .build();
        return settingsRepository.save(defaults);
    }

    private UserSettingsDTO mapToDTO(UserSettings s) {
        return UserSettingsDTO.builder()
                .general(
                        new GeneralSettingsDTO(
                                s.getAppName(),
                                s.getTimezone(),
                                s.getLanguage()))
                .security(
                        new SecuritySettingsDTO(
                                s.isTwoFactorEnabled(),
                                s.getPasswordPolicy()))
                .notifications(
                        new NotificationSettingsDTO(
                                s.isEmailApproval(),
                                s.isSms(),
                                s.isPush()))
                .build();
    }
}
