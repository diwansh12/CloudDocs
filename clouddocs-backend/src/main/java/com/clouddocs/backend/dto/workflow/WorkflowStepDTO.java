package com.clouddocs.backend.dto.workflow;

import com.clouddocs.backend.entity.StepType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * DTO for workflow step information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow step definition")
public class WorkflowStepDTO {
    
    @Schema(description = "Step unique identifier")
    private Long id;
    
    @NotNull
    @Positive
    @Schema(description = "Step execution order", example = "1")
    private Integer stepOrder;
    
    @NotNull
    @Schema(description = "Step name", example = "Manager Approval")
    private String name;
    
    @Schema(description = "Description of what happens in this step")
    private String description;
    
    @NotNull
    @Schema(description = "Type of step (APPROVAL, REVIEW, etc.)")
    private StepType stepType;
    
    @Schema(description = "SLA hours for this step", example = "24")
    private Integer slaHours;
    
    @Schema(description = "Required roles to complete this step")
    private List<String> requiredRoles;
    
    // ✅ ENHANCED: Additional useful fields
    @Schema(description = "Whether this step is required for workflow completion")
    private boolean isRequired;
    
    @Schema(description = "Whether this step can be skipped")
    private boolean canSkip;
    
    @Schema(description = "Whether this step allows parallel execution")
    private boolean allowParallel;
    
    @Schema(description = "Step status in current workflow")
    private String status; // PENDING, IN_PROGRESS, COMPLETED, SKIPPED

    // Constructors
    public WorkflowStepDTO() {}

    // ✅ ENHANCED: All getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }

    public List<String> getRequiredRoles() { return requiredRoles; }
    public void setRequiredRoles(List<String> requiredRoles) { this.requiredRoles = requiredRoles; }
    
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }
    
    public boolean isCanSkip() { return canSkip; }
    public void setCanSkip(boolean canSkip) { this.canSkip = canSkip; }
    
    public boolean isAllowParallel() { return allowParallel; }
    public void setAllowParallel(boolean allowParallel) { this.allowParallel = allowParallel; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
