package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.NotificationRepository;
import com.clouddocs.backend.repository.UserNotificationSettingsRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private UserNotificationSettingsRepository settingsRepository;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${app.email.from:noreply@clouddocs.com}")
    private String fromEmail;
    
    @Value("${app.twilio.phone:+1234567890}")
    private String twilioPhoneNumber;
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    // ✅ ADDED: Public method for test notifications
    /**
     * PUBLIC METHOD: Send test notification via all available channels
     */
    public void sendTestNotification(User user, String title, String message) {
        if (user == null) {
            logger.warn("Cannot send test notification - user is null");
            return;
        }
        
        try {
            Notification notification = new Notification(user, title, message);
            
            // Add enhanced fields if available
            if (hasEnhancedFields(notification)) {
                notification.setType(NotificationType.GENERAL);
                notification.setPriorityLevel("NORMAL");
            }
            
            // Call the private method that handles multi-channel sending
            sendMultiChannelNotification(user, notification);
            
            logger.info("✅ Test notification sent successfully to user: {}", user.getUsername());
            
        } catch (Exception e) {
            logger.error("❌ Failed to send test notification to user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to send test notification: " + e.getMessage(), e);
        }
    }

    // ✅ ADDED: Public method specifically for email testing
    /**
     * PUBLIC METHOD: Send test email notification
     */
    public boolean sendTestEmail(User user, String title, String message) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            logger.warn("Cannot send test email - user or email is null/blank");
            return false;
        }
        
        try {
            Notification notification = new Notification(user, title, message);
            notification = notificationRepository.save(notification);
            
            boolean emailSent = sendEmailNotification(user, notification);
            
            if (hasEnhancedFields(notification)) {
                notification.setSentViaEmail(emailSent);
                notificationRepository.save(notification);
            }
            
            return emailSent;
            
        } catch (Exception e) {
            logger.error("❌ Failed to send test email to user: {}", user.getUsername(), e);
            return false;
        }
    }

    // ✅ ADDED: Public method specifically for SMS testing
    /**
     * PUBLIC METHOD: Send test SMS notification
     */
    public boolean sendTestSms(User user, String title, String message) {
        if (user == null || user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            logger.warn("Cannot send test SMS - user or phone number is null/blank");
            return false;
        }
        
        try {
            Notification notification = new Notification(user, title, message);
            notification = notificationRepository.save(notification);
            
            boolean smsSent = sendSmsNotification(user, notification);
            
            if (hasEnhancedFields(notification)) {
                notification.setSentViaSms(smsSent);
                notificationRepository.save(notification);
            }
            
            return smsSent;
            
        } catch (Exception e) {
            logger.error("❌ Failed to send test SMS to user: {}", user.getUsername(), e);
            return false;
        }
    }

    // ... ALL YOUR EXISTING METHODS REMAIN THE SAME ...

    /**
     * ✅ Notify user when a task is assigned to them
     */
    public void notifyTaskAssigned(User user, WorkflowTask task) {
        if (user == null || task == null) {
            logger.warn("Cannot send task assignment notification - user or task is null");
            return;
        }

        String title = String.format("New Task: %s", safe(task.getTitle()));
        String body = String.format(
            "A new workflow task '%s' has been assigned to you for document '%s'. Please review and take appropriate action.",
            safe(task.getTitle()),
            safeDocumentName(task)
        );

        Notification notification = new Notification(user, title, body);
        
        if (hasEnhancedFields(notification)) {
            notification.setType(NotificationType.TASK_ASSIGNED);
            notification.setWorkflowId(task.getWorkflowInstance().getId());
            notification.setTaskId(task.getId());
            notification.setPriorityLevel(getTaskPriority(task));
        }
        
        sendMultiChannelNotification(user, notification);
        
        logger.info("Task assignment notification sent to user: {}", user.getUsername());
    }

    public void notifyTaskCompleted(User user, WorkflowTask task, TaskAction action) {
        if (user == null || task == null || action == null) {
            logger.warn("Cannot send task completion notification - required parameters are null");
            return;
        }

        User initiator = task.getWorkflowInstance() != null ? task.getWorkflowInstance().getInitiatedBy() : null;
        if (initiator != null && !initiator.getId().equals(user.getId())) {
            String title = String.format("Task %s: %s", 
                action.name().toLowerCase(), safe(task.getTitle()));
            String body = String.format(
                "Task '%s' for document '%s' has been %s by %s.",
                safe(task.getTitle()),
                safeDocumentName(task),
                action.name().toLowerCase(),
                safeFullName(user)
            );

            Notification notification = new Notification(initiator, title, body);
            
            if (hasEnhancedFields(notification)) {
                notification.setType(NotificationType.TASK_COMPLETED);
                notification.setWorkflowId(task.getWorkflowInstance().getId());
                notification.setTaskId(task.getId());
            }
            
            sendMultiChannelNotification(initiator, notification);
        }

        logger.info("Task completion notification recorded for task: {} with action: {}", task.getId(), action);
    }

    public void notifyWorkflowApproved(User user, WorkflowInstance instance) {
        if (user == null || instance == null) {
            logger.warn("Cannot send workflow approval notification - user or instance is null");
            return;
        }

        String title = "Workflow Approved ✅";
        String body = String.format(
            "Great news! Your workflow for document '%s' has been fully approved and completed.",
            safeDocumentName(instance)
        );

        Notification notification = new Notification(user, title, body);
        
        if (hasEnhancedFields(notification)) {
            notification.setType(NotificationType.WORKFLOW_APPROVED);
            notification.setWorkflowId(instance.getId());
            notification.setPriorityLevel("HIGH");
        }
        
        sendMultiChannelNotification(user, notification);
        
        logger.info("Workflow approval notification sent to user: {}", user.getUsername());
    }

    public void notifyWorkflowRejected(User user, WorkflowInstance instance) {
        if (user == null || instance == null) {
            logger.warn("Cannot send workflow rejection notification - user or instance is null");
            return;
        }

        String title = "Workflow Rejected ❌";
        String body = String.format(
            "Your workflow for document '%s' has been rejected. Please check the comments and take appropriate action.",
            safeDocumentName(instance)
        );

        Notification notification = new Notification(user, title, body);
        
        if (hasEnhancedFields(notification)) {
            notification.setType(NotificationType.WORKFLOW_REJECTED);
            notification.setWorkflowId(instance.getId());
            notification.setPriorityLevel("HIGH");
        }
        
        sendMultiChannelNotification(user, notification);
        
        logger.info("Workflow rejection notification sent to user: {}", user.getUsername());
    }

    public void notifyTaskOverdue(User user, WorkflowTask task) {
        if (user == null || task == null) {
            logger.warn("Cannot send overdue notification - user or task is null");
            return;
        }

        String title = String.format("Task Overdue: %s", safe(task.getTitle()));
        String body = String.format(
            "Task '%s' for document '%s' is now overdue. Please complete it as soon as possible.",
            safe(task.getTitle()),
            safeDocumentName(task)
        );

        Notification notification = new Notification(user, title, body);
        
        if (hasEnhancedFields(notification)) {
            notification.setType(NotificationType.TASK_OVERDUE);
            notification.setWorkflowId(task.getWorkflowInstance().getId());
            notification.setTaskId(task.getId());
            notification.setPriorityLevel("URGENT");
        }
        
        sendMultiChannelNotification(user, notification);
        
        logger.info("Overdue task notification sent to user: {}", user.getUsername());
    }

    /**
     * ✅ KEEP PRIVATE: Multi-channel notification sender with null safety
     */
    @Async
    private void sendMultiChannelNotification(User user, Notification notification) {
        try {
            notification = notificationRepository.save(notification);
            
            UserNotificationSettings settings = getUserSettings(user);
            
            if (settings != null && isQuietHours(settings) && !isUrgent(notification)) {
                logger.info("Skipping real-time notifications due to quiet hours for user: {}", user.getUsername());
                return;
            }
            
            if (canSendEmail(user, settings)) {
                boolean emailSent = sendEmailNotification(user, notification);
                if (hasEnhancedFields(notification)) {
                    notification.setSentViaEmail(emailSent);
                }
            }
            
            if (canSendSms(user, settings)) {
                boolean smsSent = sendSmsNotification(user, notification);
                if (hasEnhancedFields(notification)) {
                    notification.setSentViaSms(smsSent);
                }
            }
            
            if (canSendPush(user, settings)) {
                boolean pushSent = sendPushNotification(user, notification);
                if (hasEnhancedFields(notification)) {
                    notification.setSentViaPush(pushSent);
                }
            }
            
            notificationRepository.save(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send multi-channel notification to user: {}", user.getUsername(), e);
        }
    }

    private boolean sendEmailNotification(User user, Notification notification) {
        if (mailSender == null || user.getEmail() == null || user.getEmail().isBlank()) {
            logger.debug("Skipping email notification - no mail sender or user email");
            return false;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("CloudDocs - " + notification.getTitle());
            message.setText(buildEmailBody(notification, user));
            
            mailSender.send(message);
            logger.info("✅ Email sent to: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to send email to: {}", user.getEmail(), e);
            return false;
        }
    }

    private boolean sendSmsNotification(User user, Notification notification) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            logger.debug("Skipping SMS notification - no user phone number");
            return false;
        }
        
        try {
            String smsBody = String.format("CloudDocs: %s - %s", 
                notification.getTitle(), 
                truncate(notification.getBody(), 100));
            
            com.twilio.rest.api.v2010.account.Message twilioMessage = 
                com.twilio.rest.api.v2010.account.Message.creator(
                    new PhoneNumber(user.getPhoneNumber()),
                    new PhoneNumber(twilioPhoneNumber),
                    smsBody
                ).create();
            
            logger.info("✅ SMS sent to: {} with SID: {}", user.getPhoneNumber(), twilioMessage.getSid());
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to send SMS to: {}", user.getPhoneNumber(), e);
            return false;
        }
    }

    private boolean sendPushNotification(User user, Notification notification) {
        UserNotificationSettings settings = getUserSettings(user);
        if (settings == null || settings.getFcmToken() == null || settings.getFcmToken().isBlank()) {
            logger.debug("Skipping push notification - no FCM token for user: {}", user.getUsername());
            return false;
        }
        
        try {
            Map<String, String> data = new HashMap<>();
            data.put("notificationId", notification.getId().toString());
            
            if (hasEnhancedFields(notification)) {
                data.put("type", notification.getType().name());
                if (notification.getWorkflowId() != null) {
                    data.put("workflowId", notification.getWorkflowId().toString());
                    data.put("clickAction", baseUrl + "/workflow/" + notification.getWorkflowId());
                }
            }

            com.google.firebase.messaging.Notification fcmNotification = 
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(truncate(notification.getBody(), 100))
                    .build();

            Message message = Message.builder()
                .setToken(settings.getFcmToken())
                .setNotification(fcmNotification)
                .putAllData(data)
                .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("✅ Push notification sent: {}", response);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to send push notification to user: {}", user.getUsername(), e);
            return false;
        }
    }

    // ✅ ALL YOUR EXISTING HELPER METHODS
    private UserNotificationSettings getUserSettings(User user) {
        if (settingsRepository == null) return null;
        return settingsRepository.findByUser(user).orElse(null);
    }
    
    private boolean canSendEmail(User user, UserNotificationSettings settings) {
        if (mailSender == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        if (settings == null) return true;
        return settings.getEmailEnabled() != null ? settings.getEmailEnabled() : true;
    }
    
    private boolean canSendSms(User user, UserNotificationSettings settings) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            return false;
        }
        if (settings == null) return false;
        return settings.getSmsEnabled() != null ? settings.getSmsEnabled() : false;
    }
    
    private boolean canSendPush(User user, UserNotificationSettings settings) {
        if (settings == null || settings.getFcmToken() == null || settings.getFcmToken().isBlank()) {
            return false;
        }
        return settings.getPushEnabled() != null ? settings.getPushEnabled() : true;
    }
    
    private boolean isQuietHours(UserNotificationSettings settings) {
        if (settings == null) return false;
        
        try {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(settings.getQuietHoursStart() != null ? settings.getQuietHoursStart() : "22:00");
            LocalTime end = LocalTime.parse(settings.getQuietHoursEnd() != null ? settings.getQuietHoursEnd() : "08:00");
            
            if (start.isBefore(end)) {
                return now.isAfter(start) && now.isBefore(end);
            } else {
                return now.isAfter(start) || now.isBefore(end);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isUrgent(Notification notification) {
        if (!hasEnhancedFields(notification)) return false;
        
        return "URGENT".equals(notification.getPriorityLevel()) ||
               (notification.getType() != null && 
                (notification.getType() == NotificationType.SYSTEM_ALERT ||
                 notification.getType() == NotificationType.TASK_OVERDUE));
    }
    
    private boolean hasEnhancedFields(Notification notification) {
        try {
            notification.getType();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getTaskPriority(WorkflowTask task) {
        try {
            WorkflowInstance instance = task.getWorkflowInstance();
            if (instance != null && instance.getPriority() != null) {
                return instance.getPriority().toString();
            }
        } catch (Exception e) {
            logger.debug("Could not determine task priority", e);
        }
        return "NORMAL";
    }
    
    private String buildEmailBody(Notification notification, User user) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(safeFullName(user)).append(",\n\n");
        body.append(notification.getBody());
        body.append("\n\n");
        
        if (hasEnhancedFields(notification) && notification.getWorkflowId() != null) {
            body.append("View Details: ").append(baseUrl)
                .append("/workflow/").append(notification.getWorkflowId());
            body.append("\n\n");
        }
        
        body.append("To manage your notification preferences, visit: ")
            .append(baseUrl).append("/settings");
        body.append("\n\n");
        body.append("Best regards,\nCloudDocs Team");
        return body.toString();
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    private String safe(String s) { return s == null ? "" : s; }
    
    private String safeUsername(User user) {
        return (user == null || user.getUsername() == null) ? "unknown" : user.getUsername();
    }
    
    private String safeFullName(User user) {
        if (user == null) return "Unknown";
        String fullName = user.getFullName();
        return (fullName == null || fullName.isBlank()) ? safeUsername(user) : fullName;
    }
    
    private String safeDocumentName(WorkflowTask task) {
        try {
            return task.getWorkflowInstance().getDocument().getOriginalFilename();
        } catch (Exception e) {
            return "document";
        }
    }
    
    private String safeDocumentName(WorkflowInstance instance) {
        try {
            return instance.getDocument().getOriginalFilename();
        } catch (Exception e) {
            return "document";
        }
    }
}
