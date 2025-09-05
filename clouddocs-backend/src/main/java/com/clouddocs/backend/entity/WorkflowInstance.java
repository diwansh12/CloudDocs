package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private WorkflowTemplate template;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    @Enumerated(EnumType.STRING)
    private WorkflowStatus status;
    
    @Column(name = "current_step_order")
    private Integer currentStepOrder = 1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;
    
    // ✅ MIGRATED: Changed from LocalDateTime to OffsetDateTime
    @Column(name = "start_date")
    private OffsetDateTime startDate;
    
    @Column(name = "end_date")
    private OffsetDateTime endDate;
    
    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "title")
    private String title;
    
    @Column(name = "description", length = 1000)
    private String description;

    // ✅ MIGRATED: Use OffsetDateTime with proper timezone handling
    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private OffsetDateTime createdDate;

    @Column(name = "updated_date")
    private OffsetDateTime updatedDate;

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowTask> tasks = new ArrayList<>();
    
    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("actionDate ASC")
    private List<WorkflowHistory> history = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private WorkflowPriority priority = WorkflowPriority.NORMAL;

    @Version
    @Column(name = "version")
    private Long version;
    
    private String comments;
    
    // ✅ UPDATED: Constructor with OffsetDateTime
    public WorkflowInstance() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.startDate = now;
        this.createdDate = now;
        this.updatedDate = now;
        this.status = WorkflowStatus.IN_PROGRESS;
    }
    
    public WorkflowInstance(WorkflowTemplate template, Document document, User initiatedBy, Integer slaHours) {
        this(); // Call default constructor for timestamp initialization
        this.template = template;
        this.document = document;
        this.initiatedBy = initiatedBy;
        this.currentStepOrder = 1;
        if (slaHours != null) {
            this.dueDate = OffsetDateTime.now(ZoneOffset.UTC).plusHours(slaHours);
        }
    }
    
    // ✅ UPDATED: Timestamp methods with OffsetDateTime
    public void updateTimestamp() {
        this.updatedDate = OffsetDateTime.now(ZoneOffset.UTC);
    }
    
    public void updateTimestampWithReason(String reason) {
        OffsetDateTime oldTime = this.updatedDate;
        this.updatedDate = OffsetDateTime.now(ZoneOffset.UTC);
        System.out.println("Timestamp updated for workflow " + this.id + 
                          " - Reason: " + reason + 
                          " - Old: " + oldTime + 
                          " - New: " + this.updatedDate);
    }
    
    // Helper methods
    public boolean isOverdue() {
        return dueDate != null && OffsetDateTime.now(ZoneOffset.UTC).isAfter(dueDate) && 
               status == WorkflowStatus.IN_PROGRESS;
    }
    
    public boolean isCompleted() {
        return status == WorkflowStatus.APPROVED || status == WorkflowStatus.REJECTED;
    }
    
    public WorkflowTask getCurrentTask() {
        return tasks.stream()
                   .filter(task -> task.getStatus() == TaskStatus.PENDING)
                   .findFirst()
                   .orElse(null);
    }

    public OffsetDateTime getLastActivityDate() {
        if (updatedDate != null) return updatedDate;
        if (createdDate != null) return createdDate;
        return startDate;
    }
    
    // ✅ UPDATED: All getters and setters for OffsetDateTime
    public OffsetDateTime getStartDate() { return startDate; }
    public void setStartDate(OffsetDateTime startDate) { this.startDate = startDate; }
    
    public OffsetDateTime getEndDate() { return endDate; }
    public void setEndDate(OffsetDateTime endDate) { 
        this.endDate = endDate;
        updateTimestamp();
    }
    
    public OffsetDateTime getDueDate() { return dueDate; }
    public void setDueDate(OffsetDateTime dueDate) { this.dueDate = dueDate; }
    
    public OffsetDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(OffsetDateTime createdDate) { this.createdDate = createdDate; }
    
    public OffsetDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(OffsetDateTime updatedDate) { this.updatedDate = updatedDate; }
    
    // ... rest of your getters and setters remain the same
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public WorkflowTemplate getTemplate() { return template; }
    public void setTemplate(WorkflowTemplate template) { this.template = template; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { 
        this.status = status;
        updateTimestamp();
    }
    
    public Integer getCurrentStepOrder() { return currentStepOrder; }
    public void setCurrentStepOrder(Integer currentStepOrder) { 
        this.currentStepOrder = currentStepOrder;
        updateTimestamp();
    }
    
    public User getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(User initiatedBy) { this.initiatedBy = initiatedBy; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<WorkflowTask> getTasks() { return tasks; }
    public void setTasks(List<WorkflowTask> tasks) { this.tasks = tasks; }
    
    public List<WorkflowHistory> getHistory() { return history; }
    public void setHistory(List<WorkflowHistory> history) { this.history = history; }
    
    public WorkflowPriority getPriority() { return priority; }
    public void setPriority(WorkflowPriority priority) { this.priority = priority; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
