package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.NotificationService;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*")
public class NotificationTestController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * ✅ FIXED: Test email notification using public method
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> testEmail() {
        try {
            User currentUser = getCurrentUser();
            
            // ✅ FIXED: Use public method instead of private one
            boolean emailSent = notificationService.sendTestEmail(
                currentUser,
                "🧪 Email Test",
                "This is a test email from CloudDocs notification system. If you receive this, email notifications are working correctly!"
            );

            if (emailSent) {
                return ResponseEntity.ok(Map.of(
                    "message", "Test email sent successfully",
                    "recipient", currentUser.getEmail(),
                    "status", "✅ Email delivered"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "message", "Email test completed but may not have been sent",
                    "recipient", currentUser.getEmail(),
                    "status", "⚠️ Check email configuration",
                    "suggestion", "Verify SMTP settings in application.properties"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send test email: " + e.getMessage(),
                "suggestion", "Check email configuration and user setup"
            ));
        }
    }

    /**
     * ✅ FIXED: Test SMS notification using public method
     */
    @PostMapping("/sms")
    public ResponseEntity<Map<String, String>> testSms() {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser.getPhoneNumber() == null || currentUser.getPhoneNumber().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "No phone number configured for user",
                    "suggestion", "Add phone number to user profile to test SMS notifications"
                ));
            }

            // ✅ FIXED: Use public method instead of private one
            boolean smsSent = notificationService.sendTestSms(
                currentUser,
                "🧪 SMS Test",
                "CloudDocs SMS test - your notifications are working!"
            );

            if (smsSent) {
                return ResponseEntity.ok(Map.of(
                    "message", "Test SMS sent successfully",
                    "recipient", currentUser.getPhoneNumber(),
                    "status", "✅ SMS delivered"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "message", "SMS test completed but may not have been sent",
                    "recipient", currentUser.getPhoneNumber(),
                    "status", "⚠️ Check SMS configuration",
                    "suggestion", "Verify Twilio settings in application.properties"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send test SMS: " + e.getMessage(),
                "suggestion", "Check Twilio configuration and phone number format"
            ));
        }
    }

    /**
     * ✅ NEW: Test all notification channels at once
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> testAllNotifications() {
        try {
            User currentUser = getCurrentUser();
            
            // ✅ FIXED: Use public method for comprehensive testing
            notificationService.sendTestNotification(
                currentUser,
                "🧪 Multi-Channel Test",
                "This is a comprehensive test of your CloudDocs notification system. " +
                "You should receive this via in-app notification and any other enabled channels (email, SMS, push)."
            );

            // Check user's notification capabilities
            Map<String, Object> capabilities = Map.of(
                "email", currentUser.getEmail() != null && !currentUser.getEmail().isBlank(),
                "sms", currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isBlank(),
                "inApp", true,
                "push", "Depends on FCM token setup"
            );

            return ResponseEntity.ok(Map.of(
                "message", "Comprehensive notification test initiated",
                "user", currentUser.getUsername(),
                "capabilities", capabilities,
                "instructions", "Check your notifications panel, email, and phone for test messages"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send test notifications: " + e.getMessage(),
                "suggestion", "Check notification service configuration"
            ));
        }
    }

    /**
     * ✅ NEW: Get notification test status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationTestStatus() {
        try {
            User currentUser = getCurrentUser();
            
            Map<String, Object> status = Map.of(
                "user", currentUser.getUsername(),
                "email", Map.of(
                    "configured", currentUser.getEmail() != null && !currentUser.getEmail().isBlank(),
                    "address", currentUser.getEmail() != null ? currentUser.getEmail() : "Not configured"
                ),
                "sms", Map.of(
                    "configured", currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isBlank(),
                    "number", currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "Not configured"
                ),
                "services", Map.of(
                    "notificationService", "Available",
                    "emailService", "Check SMTP configuration",
                    "smsService", "Check Twilio configuration",
                    "pushService", "Check Firebase configuration"
                )
            );

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get notification status: " + e.getMessage()
            ));
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }
}
