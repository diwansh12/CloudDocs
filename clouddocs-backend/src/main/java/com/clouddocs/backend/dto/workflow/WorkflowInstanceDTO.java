package com.clouddocs.backend.dto.workflow;

import com.clouddocs.backend.entity.WorkflowPriority;
import com.clouddocs.backend.entity.WorkflowStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for workflow instance with complete details including tasks and history
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // ✅ Exclude null fields from JSON
@Schema(description = "Workflow instance with complete details")
public class WorkflowInstanceDTO {
    
    @Schema(description = "Unique workflow identifier")
    private Long id;
    
    @NotNull
    @Schema(description = "Current workflow status")
    private WorkflowStatus status;
    
    @Schema(description = "Current step order in the workflow")
    private Integer currentStepOrder;
    
    @Schema(description = "Workflow title", example = "Document Approval Workflow")
    private String title;
    
    @Schema(description = "Detailed description of the workflow")
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @Schema(description = "When the workflow started")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @Schema(description = "When the workflow ended (if completed)")
    private LocalDateTime endDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @Schema(description = "Expected completion date")
    private LocalDateTime dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdDate;
    
    @Schema(description = "Workflow priority level")
    private WorkflowPriority priority;
    
    @Schema(description = "Additional comments or notes")
    private String comments;

    // Document details
    @Schema(description = "Associated document ID")
    private Long documentId;
    
    @Schema(description = "Associated document name")
    private String documentName;
    
    // Initiator details
    @Schema(description = "User ID who initiated the workflow")
    private Long initiatedById;
    
    @Schema(description = "Full name of the user who initiated the workflow")
    private String initiatedByName;

    // Template details
    @Schema(description = "Workflow template ID")
    private UUID templateId;
    
    @Schema(description = "Workflow template name")
    private String templateName;
    
    // ✅ ENHANCED: Progress tracking fields
    @Schema(description = "Total number of tasks in workflow")
    private int totalTasks;
    
    @Schema(description = "Number of completed tasks")
    private int completedTasks;
    
    @Schema(description = "Number of pending tasks")
    private int pendingTasks;
    
    @Schema(description = "Progress percentage (0-100)")
    private int progressPercentage;
    
    // ✅ ENHANCED: Current assignment info
    @Schema(description = "Currently assigned user name")
    private String currentAssignee;
    
    @Schema(description = "Current step name")
    private String currentStepName;
    
    // ✅ ENHANCED: Timing information
    @Schema(description = "Relative time since last update (e.g., '2 hours ago')")
    private String lastUpdatedRelative;
    
    @Schema(description = "Whether workflow is overdue")
    private boolean isOverdue;

    // ✅ ADDED: User permissions field
    @Schema(description = "User-specific permissions for this workflow")
    private Map<String, Object> userPermissions;

    // Related data collections
    @Schema(description = "List of workflow tasks")
    private List<WorkflowTaskDTO> tasks;
    
    @Schema(description = "Workflow history/audit trail")
    private List<WorkflowHistoryDTO> history;
    
    @Schema(description = "Workflow step definitions")
    private List<WorkflowStepDTO> steps;

    // ✅ ENHANCED: Constructors
    public WorkflowInstanceDTO() {}

    // ✅ ENHANCED: Helper methods
    public int getProgressPercentage() {
        if (totalTasks == 0) return 0;
        return (completedTasks * 100) / totalTasks;
    }
    
    public boolean getIsOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && 
               (status == WorkflowStatus.IN_PROGRESS || status == WorkflowStatus.PENDING);
    }

    // ✅ ADD: Getters and setters for new fields
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }
    
    public String getCurrentAssignee() { return currentAssignee; }
    public void setCurrentAssignee(String currentAssignee) { this.currentAssignee = currentAssignee; }
    
    public String getCurrentStepName() { return currentStepName; }
    public void setCurrentStepName(String currentStepName) { this.currentStepName = currentStepName; }
    
    public String getLastUpdatedRelative() { return lastUpdatedRelative; }
    public void setLastUpdatedRelative(String lastUpdatedRelative) { this.lastUpdatedRelative = lastUpdatedRelative; }
    
    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }

    // ✅ ADDED: User permissions getter and setter
    public Map<String, Object> getUserPermissions() { return userPermissions; }
    public void setUserPermissions(Map<String, Object> userPermissions) { this.userPermissions = userPermissions; }

    // Your existing getters and setters remain the same...
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
