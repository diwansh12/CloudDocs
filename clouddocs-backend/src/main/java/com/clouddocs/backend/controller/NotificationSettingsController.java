package com.clouddocs.backend.controller;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.UserNotificationSettings;
import com.clouddocs.backend.entity.Notification;
import com.clouddocs.backend.repository.UserNotificationSettingsRepository;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.NotificationRepository;
import com.clouddocs.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * ✅ COMPLETE FIXED: Notification Settings Controller with all warnings resolved
 */
@RestController
@RequestMapping("/users/notification-settings")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
@PreAuthorize("isAuthenticated()")
public class NotificationSettingsController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationSettingsController.class);
    
    @Autowired
    private UserNotificationSettingsRepository settingsRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // ✅ ADDED: Notification repository for test notifications
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private NotificationService notificationService;
    
    /**
     * Get current user's notification settings
     */
    @GetMapping
    public ResponseEntity<UserNotificationSettings> getSettings() {
        User user = getCurrentUser();
        UserNotificationSettings settings = settingsRepository.findByUser(user)
            .orElse(createDefaultSettings(user));
        return ResponseEntity.ok(settings);
    }
    
    /**
     * Update user's notification settings
     */
    @PutMapping
    public ResponseEntity<UserNotificationSettings> updateSettings(
            @RequestBody UserNotificationSettings settings) {
        User user = getCurrentUser();
        
        // Ensure the settings belong to current user
        settings.setUser(user);
        
        // Find existing settings to preserve ID
        UserNotificationSettings existingSettings = settingsRepository.findByUser(user).orElse(null);
        if (existingSettings != null) {
            settings.setId(existingSettings.getId());
        }
        
        UserNotificationSettings saved = settingsRepository.save(settings);
        logger.info("Updated notification settings for user: {}", user.getUsername());
        
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Save FCM token for push notifications
     */
    @PostMapping("/fcm-token")
    public ResponseEntity<Map<String, String>> saveFcmToken(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String token = request.get("token");
        
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "FCM token is required"));
        }
        
        UserNotificationSettings settings = settingsRepository.findByUser(user)
            .orElse(createDefaultSettings(user));
        settings.setFcmToken(token);
        settingsRepository.save(settings);
        
        logger.info("FCM token saved for user: {}", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "FCM token saved successfully"));
    }
    
    /**
     * Remove FCM token (logout)
     */
    @DeleteMapping("/fcm-token")
    public ResponseEntity<Map<String, String>> removeFcmToken() {
        User user = getCurrentUser();
        
        settingsRepository.findByUser(user).ifPresent(settings -> {
            settings.setFcmToken(null);
            settingsRepository.save(settings);
            logger.info("FCM token removed for user: {}", user.getUsername());
        });
        
        return ResponseEntity.ok(Map.of("message", "FCM token removed successfully"));
    }
    
    /**
     * ✅ COMPLETELY FIXED: Test notification endpoint - RECOMMENDED SOLUTION
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification() {
        User currentUser = getCurrentUser();
        
        try {
            // ✅ FIXED: Create notification and actually use it by saving to database
            Notification testNotification = new Notification(
                currentUser, 
                "🔔 Test Notification", 
                "This is a test notification to verify your notification settings are working correctly. " +
                "If you can see this, your in-app notifications are functioning properly!"
            );
            
            // ✅ FIXED: Save the notification - this resolves the unused variable warning
            Notification savedNotification = notificationRepository.save(testNotification);
            
            // ✅ ENHANCED: Try to send via other channels if notification service is available
            boolean multiChannelSent = false;
            if (notificationService != null) {
                try {
                    // Create a simple test notification that will trigger email/SMS/push if configured
                    notificationService.notifyTaskAssigned(currentUser, createMockTaskForTesting(currentUser));
                    multiChannelSent = true;
                } catch (Exception e) {
                    logger.warn("Multi-channel notification failed, but in-app notification created: {}", e.getMessage());
                }
            }
            
            // ✅ ENHANCED: Provide detailed response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Test notification created successfully");
            response.put("notificationId", savedNotification.getId().toString());
            response.put("inAppNotification", "✅ Created");
            response.put("multiChannelNotification", multiChannelSent ? "✅ Sent" : "⚠️ Service unavailable");
            response.put("instructions", "Check your notifications panel to see the test notification");
            
            logger.info("Test notification created for user: {} with ID: {}", 
                       currentUser.getUsername(), savedNotification.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to create test notification for user: {}", currentUser.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to create test notification: " + e.getMessage(),
                "suggestion", "Please check your notification configuration"
            ));
        }
    }
    
    /**
     * Get notification preferences summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getNotificationSummary() {
        User user = getCurrentUser();
        UserNotificationSettings settings = settingsRepository.findByUser(user)
            .orElse(createDefaultSettings(user));
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("emailEnabled", settings.getEmailEnabled() != null ? settings.getEmailEnabled() : true);
        summary.put("smsEnabled", settings.getSmsEnabled() != null ? settings.getSmsEnabled() : false);
        summary.put("pushEnabled", settings.getPushEnabled() != null ? settings.getPushEnabled() : true);
        summary.put("hasFcmToken", settings.getFcmToken() != null && !settings.getFcmToken().isBlank());
        summary.put("hasPhoneNumber", user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank());
        summary.put("hasEmail", user.getEmail() != null && !user.getEmail().isBlank());
        
        Map<String, String> quietHours = new HashMap<>();
        quietHours.put("start", settings.getQuietHoursStart() != null ? settings.getQuietHoursStart() : "22:00");
        quietHours.put("end", settings.getQuietHoursEnd() != null ? settings.getQuietHoursEnd() : "08:00");
        summary.put("quietHours", quietHours);
        
        // Add notification type preferences
        Map<String, Boolean> preferences = new HashMap<>();
        preferences.put("emailTaskAssigned", settings.getEmailTaskAssigned() != null ? settings.getEmailTaskAssigned() : true);
        preferences.put("emailWorkflowApproved", settings.getEmailWorkflowApproved() != null ? settings.getEmailWorkflowApproved() : true);
        preferences.put("emailWorkflowRejected", settings.getEmailWorkflowRejected() != null ? settings.getEmailWorkflowRejected() : true);
        preferences.put("smsUrgentOnly", settings.getSmsUrgentOnly() != null ? settings.getSmsUrgentOnly() : true);
        preferences.put("pushTaskAssigned", settings.getPushTaskAssigned() != null ? settings.getPushTaskAssigned() : true);
        preferences.put("pushWorkflowUpdates", settings.getPushWorkflowUpdates() != null ? settings.getPushWorkflowUpdates() : true);
        summary.put("preferences", preferences);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Reset notification settings to default
     */
    @PostMapping("/reset")
    public ResponseEntity<UserNotificationSettings> resetToDefaults() {
        User user = getCurrentUser();
        
        // Delete existing settings
        settingsRepository.findByUser(user).ifPresent(existingSettings -> {
            settingsRepository.delete(existingSettings);
            logger.info("Reset notification settings for user: {}", user.getUsername());
        });
        
        // Create new default settings
        UserNotificationSettings defaultSettings = createDefaultSettings(user);
        UserNotificationSettings saved = settingsRepository.save(defaultSettings);
        
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Get notification statistics for current user
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        User user = getCurrentUser();
        
        try {
            long totalNotifications = notificationRepository.count();
            long unreadCount = notificationRepository.countByUserAndReadFlagFalse(user);
            long userTotalNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(user).size();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNotifications", userTotalNotifications);
            stats.put("unreadCount", unreadCount);
            stats.put("readCount", userTotalNotifications - unreadCount);
            stats.put("systemTotalNotifications", totalNotifications);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Failed to get notification stats for user: {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve notification statistics"));
        }
    }
    
    /**
     * ✅ Helper methods
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
    }
    
    private UserNotificationSettings createDefaultSettings(User user) {
        UserNotificationSettings settings = new UserNotificationSettings(user);
        
        // Set default values if not already set in constructor
        if (settings.getEmailEnabled() == null) settings.setEmailEnabled(true);
        if (settings.getSmsEnabled() == null) settings.setSmsEnabled(false);
        if (settings.getPushEnabled() == null) settings.setPushEnabled(true);
        if (settings.getEmailTaskAssigned() == null) settings.setEmailTaskAssigned(true);
        if (settings.getEmailWorkflowApproved() == null) settings.setEmailWorkflowApproved(true);
        if (settings.getEmailWorkflowRejected() == null) settings.setEmailWorkflowRejected(true);
        if (settings.getSmsUrgentOnly() == null) settings.setSmsUrgentOnly(true);
        if (settings.getPushTaskAssigned() == null) settings.setPushTaskAssigned(true);
        if (settings.getPushWorkflowUpdates() == null) settings.setPushWorkflowUpdates(true);
        if (settings.getQuietHoursStart() == null) settings.setQuietHoursStart("22:00");
        if (settings.getQuietHoursEnd() == null) settings.setQuietHoursEnd("08:00");
        
        return settings;
    }
    
    /**
     * ✅ HELPER: Create a mock task for testing notifications
     * This is used to test the multi-channel notification system
     */
    private com.clouddocs.backend.entity.WorkflowTask createMockTaskForTesting(User user) {
        try {
            // Create a minimal mock task for testing purposes
            // Note: This is a simplified implementation for testing only
            com.clouddocs.backend.entity.WorkflowTask mockTask = 
                new com.clouddocs.backend.entity.WorkflowTask();
            
            // Set basic properties needed for notification testing
            mockTask.setTitle("Test Task - Notification Verification");
            mockTask.setAssignedTo(user);
            
            // Create a mock workflow instance
            com.clouddocs.backend.entity.WorkflowInstance mockInstance = 
                new com.clouddocs.backend.entity.WorkflowInstance();
            mockInstance.setId(999999L); // Use a clearly test ID
            mockInstance.setInitiatedBy(user);
            
            // Create a mock document
            com.clouddocs.backend.entity.Document mockDocument = 
                new com.clouddocs.backend.entity.Document();
            mockDocument.setOriginalFilename("test-notification-document.pdf");
            
            mockInstance.setDocument(mockDocument);
            mockTask.setWorkflowInstance(mockInstance);
            
            return mockTask;
            
        } catch (Exception e) {
            logger.warn("Could not create mock task for testing: {}", e.getMessage());
            return null;
        }
    }
}
