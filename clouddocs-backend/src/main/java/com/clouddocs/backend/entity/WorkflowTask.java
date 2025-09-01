package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_tasks")
public class WorkflowTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id")
    private WorkflowInstance workflowInstance;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id")
    private WorkflowStep workflowStep;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    // ✅ ADD: Alternative field name for createdAt
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    
    // ✅ ADD: Alternative field name for completedAt
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;
    
    @Enumerated(EnumType.STRING)
    private TaskAction action;
    
    private String comments;
    
    @Enumerated(EnumType.STRING)
    private TaskPriority priority = TaskPriority.NORMAL;
    
    @Version
    @Column(name = "version")
    private Long version;

    // Constructors
    public WorkflowTask() {
        this.createdDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
    
    public WorkflowTask(WorkflowInstance workflowInstance, WorkflowStep workflowStep, 
                       User assignedTo, String title) {
        this();
        this.workflowInstance = workflowInstance;
        this.workflowStep = workflowStep;
        this.assignedTo = assignedTo;
        this.title = title;
        this.description = "Please review and approve/reject the document: " + 
                          workflowInstance.getDocument().getOriginalFilename();
        
        if (workflowStep.getSlaHours() != null) {
            this.dueDate = LocalDateTime.now().plusHours(workflowStep.getSlaHours());
        }
    }
    
    // Helper methods
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && 
               status == TaskStatus.PENDING;
    }
    
    public void complete(TaskAction action, String comments, User completedBy) {
        this.action = action;
        this.comments = comments;
        this.completedBy = completedBy;
        this.completedDate = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
        this.status = TaskStatus.COMPLETED;
    }
    
    public String getName() {
        return this.title != null ? this.title : "Unnamed Task";
    }
    
    // ===== GETTERS AND SETTERS =====
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }
    
    public WorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(WorkflowStep workflowStep) { this.workflowStep = workflowStep; }
    
    // ✅ ADD: Alias methods for step
    public WorkflowStep getStep() { return this.workflowStep; }
    public void setStep(WorkflowStep step) { this.workflowStep = step; }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { 
        this.createdDate = createdDate; 
        this.createdAt = createdDate; // Keep both in sync
    }
    
    // ✅ ADD: Alias methods for createdAt
    public LocalDateTime getCreatedAt() { return this.createdAt != null ? this.createdAt : this.createdDate; }
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
        if (this.createdDate == null) this.createdDate = createdAt; // Keep both in sync
    }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { 
        this.completedDate = completedDate; 
        this.completedAt = completedDate; // Keep both in sync
    }
    
    // ✅ ADD: Alias methods for completedAt
    public LocalDateTime getCompletedAt() { return this.completedAt != null ? this.completedAt : this.completedDate; }
    public void setCompletedAt(LocalDateTime completedAt) { 
        this.completedAt = completedAt; 
        if (this.completedDate == null) this.completedDate = completedAt; // Keep both in sync
    }
    
    public User getCompletedBy() { return completedBy; }
    public void setCompletedBy(User completedBy) { this.completedBy = completedBy; }
    
    public TaskAction getAction() { return action; }
    public void setAction(TaskAction action) { this.action = action; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
