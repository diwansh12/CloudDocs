package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
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

    // ✅ ADD: Missing audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ✅ CRITICAL: Add @JsonIgnore to prevent serialization issues
   @OneToMany(mappedBy = "template", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)

    @OrderBy("stepOrder ASC")
    @JsonIgnore  // ✅ Prevents Jackson from trying to serialize lazy collection
    private List<WorkflowStep> steps;
    
    // Constructors
    public WorkflowTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public WorkflowTemplate(String name, String description, WorkflowType type) {
        this();
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // All existing getters and setters...
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

    // ✅ ADD: Missing getters and setters for audit fields
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
