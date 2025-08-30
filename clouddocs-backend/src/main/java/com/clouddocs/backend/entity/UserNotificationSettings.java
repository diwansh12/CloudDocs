package com.clouddocs.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    // ✅ General channel preferences
    @Column(name = "email_enabled", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean emailEnabled = true;
    
    @Column(name = "sms_enabled", columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean smsEnabled = false;
    
    @Column(name = "push_enabled", columnDefinition = "BOOLEAN DEFAULT true") 
    private Boolean pushEnabled = true;
    
    // ✅ Specific notification type preferences
    @Column(name = "email_task_assigned", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean emailTaskAssigned = true;
    
    @Column(name = "email_workflow_approved", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean emailWorkflowApproved = true;
    
    @Column(name = "email_workflow_rejected", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean emailWorkflowRejected = true;
    
    @Column(name = "sms_urgent_only", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean smsUrgentOnly = true;
    
    @Column(name = "push_task_assigned", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean pushTaskAssigned = true;
    
    @Column(name = "push_workflow_updates", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean pushWorkflowUpdates = true;
    
    // ✅ Push notification token
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;
    
    @Column(name = "quiet_hours_start")
    private String quietHoursStart = "22:00";
    
    @Column(name = "quiet_hours_end")
    private String quietHoursEnd = "08:00";
    
    // Constructors
    public UserNotificationSettings() {}
    
    public UserNotificationSettings(User user) {
        this.user = user;
    }
    
    // ✅ All getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    
    public Boolean getSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(Boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    
    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    
    public Boolean getEmailTaskAssigned() { return emailTaskAssigned; }
    public void setEmailTaskAssigned(Boolean emailTaskAssigned) { this.emailTaskAssigned = emailTaskAssigned; }
    
    public Boolean getEmailWorkflowApproved() { return emailWorkflowApproved; }
    public void setEmailWorkflowApproved(Boolean emailWorkflowApproved) { this.emailWorkflowApproved = emailWorkflowApproved; }
    
    public Boolean getEmailWorkflowRejected() { return emailWorkflowRejected; }
    public void setEmailWorkflowRejected(Boolean emailWorkflowRejected) { this.emailWorkflowRejected = emailWorkflowRejected; }
    
    public Boolean getSmsUrgentOnly() { return smsUrgentOnly; }
    public void setSmsUrgentOnly(Boolean smsUrgentOnly) { this.smsUrgentOnly = smsUrgentOnly; }
    
    public Boolean getPushTaskAssigned() { return pushTaskAssigned; }
    public void setPushTaskAssigned(Boolean pushTaskAssigned) { this.pushTaskAssigned = pushTaskAssigned; }
    
    public Boolean getPushWorkflowUpdates() { return pushWorkflowUpdates; }
    public void setPushWorkflowUpdates(Boolean pushWorkflowUpdates) { this.pushWorkflowUpdates = pushWorkflowUpdates; }
    
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
}
