package com.clouddocs.backend.dto;

import com.clouddocs.backend.dto.workflow.WorkflowStepDTO;
import java.util.UUID;
import java.util.List;

public class WorkflowTemplateDto {
    private UUID id;
    private String name;
    private String description;
    private Boolean isActive;
    private String type;
    private Integer defaultSlaHours;
    private List<WorkflowStepDTO> steps;
    
    // Constructors
    public WorkflowTemplateDto() {}
    
    public WorkflowTemplateDto(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    public WorkflowTemplateDto(UUID id, String name, String description, Boolean isActive, String type, Integer defaultSlaHours) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.type = type;
        this.defaultSlaHours = defaultSlaHours;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Integer getDefaultSlaHours() { return defaultSlaHours; }
    public void setDefaultSlaHours(Integer defaultSlaHours) { this.defaultSlaHours = defaultSlaHours; }
    
    public List<WorkflowStepDTO> getSteps() { return steps; }
    public void setSteps(List<WorkflowStepDTO> steps) { this.steps = steps; }
}
