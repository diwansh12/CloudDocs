package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.LinkedHashSet;

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

    // ✅ AUDIT FIELDS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ✅ FIXED: Changed List to LinkedHashSet to avoid MultipleBagFetchException
    // LinkedHashSet maintains insertion order like a List
    @OneToMany(mappedBy = "template", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC") // ✅ Ensures consistent ordering
    @JsonIgnore // ✅ Prevents Jackson serialization issues
    private Set<WorkflowStep> steps = new LinkedHashSet<>();
    
    // ===== CONSTRUCTORS =====
    
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
    
    // ✅ ENHANCED: Safe accessor method for steps
    public Set<WorkflowStep> getStepsSafe() {
        try {
            return steps != null ? steps : new LinkedHashSet<>();
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }

    // ===== JPA LIFECYCLE METHODS =====
    
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
    
    // ===== GETTERS AND SETTERS =====
    
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
    
    // ✅ UPDATED: Changed return type from List to Set
    public Set<WorkflowStep> getSteps() { return steps; }
    public void setSteps(Set<WorkflowStep> steps) { this.steps = steps; }
    
    public Integer getDefaultSlaHours() { return defaultSlaHours; }
    public void setDefaultSlaHours(Integer defaultSlaHours) { this.defaultSlaHours = defaultSlaHours; }

    // ✅ AUDIT FIELD GETTERS AND SETTERS
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== HELPER METHODS =====
    
    /**
     * ✅ NEW: Add a step to the workflow template
     */
    public void addStep(WorkflowStep step) {
        if (step != null) {
            steps.add(step);
            step.setTemplate(this);
        }
    }
    
    /**
     * ✅ NEW: Remove a step from the workflow template
     */
    public void removeStep(WorkflowStep step) {
        if (step != null) {
            steps.remove(step);
            step.setTemplate(null);
        }
    }

    // ===== EQUALS AND HASHCODE =====
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowTemplate)) return false;
        WorkflowTemplate that = (WorkflowTemplate) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WorkflowTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", type=" + type +
                ", stepCount=" + (steps != null ? steps.size() : 0) +
                '}';
    }
}

