package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
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
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // ✅ FIXED: Added missing title and description fields
    @Column(name = "title")
    private String title;
    
    @Column(name = "description", length = 1000)
    private String description;

    // ✅ ADD THESE TIMESTAMP FIELDS
    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // Tasks for this workflow instance
    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowTask> tasks = new ArrayList<>();
    
    // Workflow history/audit trail
    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("actionDate ASC")
    private List<WorkflowHistory> history = new ArrayList<>();
    
    // Priority level
    @Enumerated(EnumType.STRING)
    private WorkflowPriority priority = WorkflowPriority.NORMAL;

    @Version
    @Column(name = "version")
    private Long version;
    
    // Comments/notes about the workflow
    private String comments;
    
    // Constructors
    public WorkflowInstance() {
        this.startDate = LocalDateTime.now();
        this.status = WorkflowStatus.IN_PROGRESS;
    }
    
    public WorkflowInstance(WorkflowTemplate template, Document document, User initiatedBy, Integer slaHours) {
        this.template = template;
        this.document = document;
        this.initiatedBy = initiatedBy;
        this.startDate = LocalDateTime.now();
        this.status = WorkflowStatus.IN_PROGRESS;
        this.currentStepOrder = 1;
        if (slaHours != null) {
            this.dueDate = LocalDateTime.now().plusHours(slaHours);
        }
    }
    
    // Helper methods
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && 
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

    // ✅ ADD HELPER METHOD FOR FRONTEND
    public LocalDateTime getLastActivityDate() {
        if (updatedDate != null) return updatedDate;
        if (createdDate != null) return createdDate;
        return startDate; // fallback
    }
    
    // ✅ FIXED: Helper method for String-to-enum conversion
    public void setPriorityFromString(String priorityStr) {
        if (priorityStr == null || priorityStr.trim().isEmpty()) {
            this.priority = WorkflowPriority.NORMAL;
            return;
        }
        
        try {
            this.priority = WorkflowPriority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.priority = WorkflowPriority.NORMAL; // Default fallback
        }
    }
    
    // ✅ GETTERS AND SETTERS - All fields included
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public WorkflowTemplate getTemplate() { return template; }
    public void setTemplate(WorkflowTemplate template) { this.template = template; }
    
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
    
    public Integer getCurrentStepOrder() { return currentStepOrder; }
    public void setCurrentStepOrder(Integer currentStepOrder) { this.currentStepOrder = currentStepOrder; }
    
    public User getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(User initiatedBy) { this.initiatedBy = initiatedBy; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    // ✅ FIXED: Added missing getters/setters for title and description
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    
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
