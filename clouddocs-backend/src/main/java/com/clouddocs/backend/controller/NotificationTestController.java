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
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class NotificationTestController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * ✅ KEEP: Test email notification
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> testEmail() {
        try {
            User currentUser = getCurrentUser();
            
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
     * ✅ UPDATED: SMS test disabled (Twilio removed for memory optimization)
     */
    @PostMapping("/sms")
    public ResponseEntity<Map<String, String>> testSms() {
        return ResponseEntity.ok(Map.of(
            "message", "SMS notifications have been disabled",
            "status", "ℹ️ Feature disabled",
            "reason", "Twilio SMS removed for memory optimization on 512MB deployment",
            "alternative", "Email notifications are available and fully functional",
            "suggestion", "Use /test/email endpoint to test notifications"
        ));
    }

    /**
     * ✅ UPDATED: Test available notification channels (email only)
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> testAllNotifications() {
        try {
            User currentUser = getCurrentUser();
            
            // Send test notification via available channels (email + in-app)
            notificationService.sendTestNotification(
                currentUser,
                "🧪 Multi-Channel Test",
                "This is a comprehensive test of your CloudDocs notification system. " +
                "You should receive this via in-app notification and email (SMS disabled for memory optimization)."
            );

            // Check user's notification capabilities (SMS disabled)
            Map<String, Object> capabilities = Map.of(
                "email", currentUser.getEmail() != null && !currentUser.getEmail().isBlank(),
                "sms", false, // Always false - SMS disabled
                "inApp", true,
                "push", "Depends on FCM token setup"
            );

            return ResponseEntity.ok(Map.of(
                "message", "Notification test initiated for available channels",
                "user", currentUser.getUsername(),
                "capabilities", capabilities,
                "note", "SMS disabled for memory optimization - email notifications active",
                "instructions", "Check your notifications panel and email for test messages"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send test notifications: " + e.getMessage(),
                "suggestion", "Check notification service configuration"
            ));
        }
    }

    /**
     * ✅ UPDATED: Get notification test status (SMS disabled)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationTestStatus() {
        try {
            User currentUser = getCurrentUser();
            
            Map<String, Object> status = Map.of(
                "user", currentUser.getUsername(),
                "email", Map.of(
                    "configured", currentUser.getEmail() != null && !currentUser.getEmail().isBlank(),
                    "address", currentUser.getEmail() != null ? currentUser.getEmail() : "Not configured",
                    "status", "Available"
                ),
                "sms", Map.of(
                    "configured", false,
                    "number", "SMS disabled for memory optimization",
                    "status", "Disabled"
                ),
                "services", Map.of(
                    "notificationService", "Available",
                    "emailService", "Available - check SMTP configuration",
                    "smsService", "Disabled - Twilio removed for memory optimization",
                    "pushService", "Available - check Firebase configuration"
                ),
                "optimization", Map.of(
                    "reason", "Memory optimization for 512MB deployment limit",
                    "memorySaved", "~40-60MB by removing Twilio SMS functionality",
                    "alternativeChannels", "Email and push notifications available"
                )
            );

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get notification status: " + e.getMessage()
            ));
        }
    }

    /**
     * ✅ NEW: Get memory optimization info
     */
    @GetMapping("/optimization-info")
    public ResponseEntity<Map<String, Object>> getOptimizationInfo() {
        Map<String, Object> info = Map.of(
            "memoryOptimization", Map.of(
                "targetLimit", "512MB (Render free tier)",
                "removedFeatures", "Twilio SMS notifications",
                "memorySaved", "~40-60MB",
                "remainingFeatures", "Email notifications, Push notifications, In-app notifications"
            ),
            "availableTestEndpoints", Map.of(
                "GET /test/status", "Check notification configuration status",
                "POST /test/email", "Test email notifications",
                "POST /test/all", "Test all available notification channels",
                "POST /test/sms", "Returns SMS disabled message"
            ),
            "recommendations", Map.of(
                "primaryChannel", "Email notifications (fully functional)",
                "testing", "Use /test/email for notification testing",
                "production", "Consider upgrading deployment tier for SMS if needed"
            )
        );

        return ResponseEntity.ok(info);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }
}
