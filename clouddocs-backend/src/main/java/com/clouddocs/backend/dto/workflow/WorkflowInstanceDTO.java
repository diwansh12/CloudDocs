package com.clouddocs.backend.dto.workflow;

import com.clouddocs.backend.entity.WorkflowPriority;
import com.clouddocs.backend.entity.WorkflowStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for workflow instance with complete details including tasks and history
 */
public class WorkflowInstanceDTO {
    
    private Long id;
    private WorkflowStatus status;
    private Integer currentStepOrder;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime updatedDate;
 @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate; 
    
    private WorkflowPriority priority;
    private String comments;

    // Document details
    private Long documentId;
    private String documentName;
    
    // Initiator details
    private Long initiatedById;
    private String initiatedByName;

    // Related data
    private List<WorkflowTaskDTO> tasks;
    private List<WorkflowHistoryDTO> history;
    private List<WorkflowStepDTO> steps;

    // Constructors
    public WorkflowInstanceDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public Integer getCurrentStepOrder() { return currentStepOrder; }
    public void setCurrentStepOrder(Integer currentStepOrder) { this.currentStepOrder = currentStepOrder; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public WorkflowPriority getPriority() { return priority; }
    public void setPriority(WorkflowPriority priority) { this.priority = priority; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public Long getInitiatedById() { return initiatedById; }
    public void setInitiatedById(Long initiatedById) { this.initiatedById = initiatedById; }

    public String getInitiatedByName() { return initiatedByName; }
    public void setInitiatedByName(String initiatedByName) { this.initiatedByName = initiatedByName; }

    public List<WorkflowTaskDTO> getTasks() { return tasks; }
    public void setTasks(List<WorkflowTaskDTO> tasks) { this.tasks = tasks; }

    public List<WorkflowHistoryDTO> getHistory() { return history; }
    public void setHistory(List<WorkflowHistoryDTO> history) { this.history = history; }

    public List<WorkflowStepDTO> getSteps() { return steps; }
    public void setSteps(List<WorkflowStepDTO> steps) { this.steps = steps; }
}
