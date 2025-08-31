package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Entity
@Table(name = "workflow_templates")
public class WorkflowTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Enumerated(EnumType.STRING)
    private WorkflowType type;
    
    @Column(name = "default_sla_hours")
    private Integer defaultSlaHours;
    
    // ✅ CRITICAL: Add @JsonIgnore to prevent serialization issues
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @JsonIgnore  // ✅ Prevents Jackson from trying to serialize lazy collection
    private List<WorkflowStep> steps;
    
    // Constructors
    public WorkflowTemplate() {}
    
    public WorkflowTemplate(String name, String description, WorkflowType type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }
    
    // ✅ Add safe accessor method
    public List<WorkflowStep> getStepsSafe() {
        try {
            return steps != null ? steps : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    // All your existing getters and setters remain the same...
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public WorkflowType getType() { return type; }
    public void setType(WorkflowType type) { this.type = type; }
    
    public List<WorkflowStep> getSteps() { return steps; }
    public void setSteps(List<WorkflowStep> steps) { this.steps = steps; }
    
    public Integer getDefaultSlaHours() { return defaultSlaHours; }
    public void setDefaultSlaHours(Integer defaultSlaHours) { this.defaultSlaHours = defaultSlaHours; }
}
