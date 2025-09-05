package com.clouddocs.backend.dto.workflow;

import com.clouddocs.backend.entity.TaskAction;
import com.clouddocs.backend.entity.TaskPriority;
import com.clouddocs.backend.entity.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime; // ✅ CHANGED: Use OffsetDateTime instead of LocalDateTime
import java.time.ZoneOffset;

/**
 * DTO for workflow task details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow task with complete details")
public class WorkflowTaskDTO {
    
    @Schema(description = "Task unique identifier")
    private Long id;
    
    @NotNull
    @Schema(description = "Task title", example = "Review Document")
    private String title;
    
    @Schema(description = "Detailed task description")
    private String description;
    
    @NotNull
    @Schema(description = "Current task status")
    private TaskStatus status;
    
    @Schema(description = "Action taken on task")
    private TaskAction action;
    
    @Schema(description = "Task priority level")
    private TaskPriority priority;
    
    // ✅ CHANGED: Use OffsetDateTime for proper timezone handling
    @Schema(description = "Task creation timestamp")
    private OffsetDateTime createdDate;
    
    @Schema(description = "Task due date")
    private OffsetDateTime dueDate;
    
    @Schema(description = "Task completion timestamp")
    private OffsetDateTime completedDate;

    // Assignee details
    @Schema(description = "ID of assigned user")
    private Long assignedToId;
    
    @Schema(description = "Name of assigned user")
    private String assignedToName;
    
    // Step details
    @Schema(description = "Step order in workflow")
    private Integer stepOrder;
    
    @Schema(description = "Step name")
    private String stepName;
    
    // ✅ ENHANCED: Additional useful fields
    @Schema(description = "Task comments or notes")
    private String comments;
    
    @Schema(description = "Whether task is overdue")
    private boolean isOverdue;
    
    @Schema(description = "Whether current user can approve this task")
    private boolean canApprove;
    
    @Schema(description = "Whether current user can reject this task")
    private boolean canReject;
    
    @Schema(description = "Whether current user can edit this task")
    private boolean canEdit;
    
    @Schema(description = "Time remaining until due date")
    private String timeRemaining;
    
    @Schema(description = "Relative time since creation")
    private String createdRelative;
    
    @Schema(description = "Workflow instance ID this task belongs to")
    private Long workflowInstanceId;

    // Constructors
    public WorkflowTaskDTO() {}

    // ✅ UPDATED: Helper methods with OffsetDateTime
    public boolean getIsOverdue() {
        return dueDate != null && OffsetDateTime.now(ZoneOffset.UTC).isAfter(dueDate) && 
               status != TaskStatus.COMPLETED;
    }

    // ✅ UPDATED: All getters and setters for OffsetDateTime
    public OffsetDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(OffsetDateTime createdDate) { this.createdDate = createdDate; }

    public OffsetDateTime getDueDate() { return dueDate; }
    public void setDueDate(OffsetDateTime dueDate) { this.dueDate = dueDate; }

    public OffsetDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(OffsetDateTime completedDate) { this.completedDate = completedDate; }

    // All other getters and setters remain the same
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskAction getAction() { return action; }
    public void setAction(TaskAction action) { this.action = action; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    
    // Enhanced field getters/setters
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }
    
    public boolean isCanApprove() { return canApprove; }
    public void setCanApprove(boolean canApprove) { this.canApprove = canApprove; }
    
    public boolean isCanReject() { return canReject; }
    public void setCanReject(boolean canReject) { this.canReject = canReject; }
    
    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }
    
    public String getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(String timeRemaining) { this.timeRemaining = timeRemaining; }
    
    public String getCreatedRelative() { return createdRelative; }
    public void setCreatedRelative(String createdRelative) { this.createdRelative = createdRelative; }
    
    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
}

