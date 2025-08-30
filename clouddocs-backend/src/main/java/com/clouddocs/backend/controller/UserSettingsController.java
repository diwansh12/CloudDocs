package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.settings.GeneralSettingsDTO;
import com.clouddocs.backend.dto.settings.SecuritySettingsDTO;
import com.clouddocs.backend.dto.settings.UserSettingsDTO;
import com.clouddocs.backend.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ✅ NEW: Controller for /api/users/settings endpoints
 * This matches your frontend API calls
 */
@Slf4j
@RestController
@RequestMapping("/api/users/settings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserSettingsController {

    private final SettingsService settingsService;

    /**
     * ✅ GET /api/users/settings/general
     */
    @GetMapping("/general")
    public ResponseEntity<Map<String, Object>> getGeneralSettings() {
        log.info("📥 GET /api/users/settings/general");
        
        try {
            UserSettingsDTO userSettings = settingsService.getCurrentUserSettings();
            GeneralSettingsDTO general = userSettings.getGeneral();
            
            Map<String, Object> response = new HashMap<>();
            response.put("appName", general != null ? general.getAppName() : "CloudDocs");
            response.put("timezone", general != null ? general.getTimezone() : "UTC");
            response.put("language", general != null ? general.getLanguage() : "English");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting general settings", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to retrieve general settings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * ✅ PUT /api/users/settings/general
     */
    @PutMapping("/general")
    public ResponseEntity<Map<String, Object>> updateGeneralSettings(
            @RequestBody Map<String, Object> settingsMap) {
        log.info("📤 PUT /api/users/settings/general: {}", settingsMap);
        
        try {
            // Convert Map to DTO
            GeneralSettingsDTO dto = new GeneralSettingsDTO();
            dto.setAppName((String) settingsMap.get("appName"));
            dto.setTimezone((String) settingsMap.get("timezone"));
            dto.setLanguage((String) settingsMap.get("language"));
            
            settingsService.updateGeneral(dto);
            
            return ResponseEntity.ok(Map.of(
                "message", "General settings updated successfully",
                "status", "success"
            ));
            
        } catch (Exception e) {
            log.error("❌ Error updating general settings", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to update general settings",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * ✅ GET /api/users/settings/security
     */
   @GetMapping("/security")
public ResponseEntity<Map<String, Object>> getSecuritySettings() {
    log.info("📥 GET /api/users/settings/security");
    
    try {
        UserSettingsDTO userSettings = settingsService.getCurrentUserSettings();
        SecuritySettingsDTO security = userSettings.getSecurity();
        
        Map<String, Object> response = new HashMap<>();
        // ✅ FIXED: Use isTwoFactorEnabled() instead of getTwoFactorEnabled()
        response.put("twoFactorEnabled", security != null ? security.isTwoFactorEnabled() : false);
        response.put("passwordPolicy", security != null ? security.getPasswordPolicy() : "Strong");
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        log.error("❌ Error getting security settings", e);
        return ResponseEntity.status(500).body(Map.of(
            "error", "Failed to retrieve security settings",
            "message", e.getMessage()
        ));
    }
}

/**
 * ✅ FIXED: PUT /api/users/settings/security
 */
@PutMapping("/security")
public ResponseEntity<Map<String, Object>> updateSecuritySettings(
        @RequestBody Map<String, Object> settingsMap) {
    log.info("📤 PUT /api/users/settings/security: {}", settingsMap);
    
    try {
        // Convert Map to DTO
        SecuritySettingsDTO dto = new SecuritySettingsDTO();
        // ✅ FIXED: Use setTwoFactorEnabled() (this one is correct)
        dto.setTwoFactorEnabled((Boolean) settingsMap.get("twoFactorEnabled"));
        dto.setPasswordPolicy((String) settingsMap.get("passwordPolicy"));
        
        settingsService.updateSecurity(dto);
        
        return ResponseEntity.ok(Map.of(
            "message", "Security settings updated successfully", 
            "status", "success"
        ));
        
    } catch (Exception e) {
        log.error("❌ Error updating security settings", e);
        return ResponseEntity.status(500).body(Map.of(
            "error", "Failed to update security settings",
            "message", e.getMessage()
        ));
    }
}

    /**
     * ✅ GET /api/users/settings (get all settings)
     */
    @GetMapping
    public ResponseEntity<UserSettingsDTO> getAllSettings() {
        log.info("📥 GET /api/users/settings");
        
        try {
            UserSettingsDTO settings = settingsService.getCurrentUserSettings();
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("❌ Error getting all settings", e);
            throw e; // Let global exception handler deal with it
        }
    }
}
