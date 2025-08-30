package com.clouddocs.backend.dto.workflow;

import com.clouddocs.backend.entity.StepType;
import java.util.List;

/**
 * DTO for workflow step information
 */
public class WorkflowStepDTO {
    
    private Long id;
    private Integer stepOrder;
    private String name;
    private StepType stepType;
    private Integer slaHours;
    private List<String> requiredRoles;

    // Constructors
    public WorkflowStepDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }

    public List<String> getRequiredRoles() { return requiredRoles; }
    public void setRequiredRoles(List<String> requiredRoles) { this.requiredRoles = requiredRoles; }
}
