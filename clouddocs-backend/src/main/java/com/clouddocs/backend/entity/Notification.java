package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Enhanced entity for multi-channel notifications
 */
@Entity
@Table(name = "notifications", 
       indexes = {
           @Index(name = "idx_notifications_user_created", 
                  columnList = "user_id, created_at"),
           @Index(name = "idx_notifications_type", 
                  columnList = "notification_type"),
           @Index(name = "idx_notifications_workflow", 
                  columnList = "workflow_id")
       })
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String body;

    @Column(name = "read_flag")
    private boolean readFlag = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ✅ NEW: Enhanced fields for multi-channel support
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private NotificationType type = NotificationType.GENERAL;

    @Column(name = "workflow_id")
    private Long workflowId;
    
    @Column(name = "task_id") 
    private Long taskId;

    @Column(name = "sent_via_email")
    private Boolean sentViaEmail = false;
    
    @Column(name = "sent_via_sms")
    private Boolean sentViaSms = false;
    
    @Column(name = "sent_via_push")
    private Boolean sentViaPush = false;

    @Column(name = "priority_level")
    private String priorityLevel = "NORMAL";

    // Constructors
    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    public Notification(User user, String title, String body) {
        this();
        this.user = user;
        this.title = title;
        this.body = body;
    }

    public Notification(User user, String title, String body, NotificationType type) {
        this(user, title, body);
        this.type = type;
    }

    // ✅ All getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Boolean getSentViaEmail() { return sentViaEmail; }
    public void setSentViaEmail(Boolean sentViaEmail) { this.sentViaEmail = sentViaEmail; }

    public Boolean getSentViaSms() { return sentViaSms; }
    public void setSentViaSms(Boolean sentViaSms) { this.sentViaSms = sentViaSms; }

    public Boolean getSentViaPush() { return sentViaPush; }
    public void setSentViaPush(Boolean sentViaPush) { this.sentViaPush = sentViaPush; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
}
