package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a step in a workflow template.
 * Each step defines approval requirements, assignees, and business rules.
 */
@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== RELATIONSHIPS =====

    /**
     * Parent workflow template that contains this step
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowTemplate template;

    /**
     * Role-based approvers for this step
     */
    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkflowStepRole> roles = new ArrayList<>();

    /**
     * Direct user assignments as approvers (alternative to role-based)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "workflow_step_approvers",
        joinColumns = @JoinColumn(name = "step_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> assignedApprovers = new ArrayList<>();

    // ===== BASIC PROPERTIES =====

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false)
    private StepType stepType = StepType.APPROVAL;

    // ✅ ADD: Missing assigneeRole field
    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_role")
    private Role assigneeRole;

    // ✅ ADD: Missing isRequired field
    @Column(name = "is_required")
    private Boolean isRequired = true;

    // ===== APPROVAL CONFIGURATION =====

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_policy")
    private ApprovalPolicy approvalPolicy = ApprovalPolicy.QUORUM;

    @Column(name = "required_approvals")
    private Integer requiredApprovals = 1;

    // ===== SLA AND AUTOMATION =====

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(name = "auto_approve_condition")
    private String autoApproveCondition;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ===== CONSTRUCTORS =====

    public WorkflowStep() {}

    public WorkflowStep(String name, Integer stepOrder, StepType stepType) {
        this.name = name;
        this.stepOrder = stepOrder;
        this.stepType = stepType;
    }

    public WorkflowStep(WorkflowTemplate template, String name, Integer stepOrder, StepType stepType) {
        this.template = template;
        this.name = name;
        this.stepOrder = stepOrder;
        this.stepType = stepType;
    }

    // ===== HELPER METHODS =====

    /**
     * Add a role-based approver to this step
     */
    public void addRole(Role role) {
        if (role == null) return;
        
        // Check if role already exists
        boolean exists = roles.stream()
            .anyMatch(stepRole -> stepRole.getRoleName() == role);
        
        if (!exists) {
            WorkflowStepRole stepRole = new WorkflowStepRole(this, role);
            this.roles.add(stepRole);
        }
    }

    /**
     * Remove a role from this step
     */
    public void removeRole(Role role) {
        if (role == null) return;
        
        roles.removeIf(stepRole -> stepRole.getRoleName() == role);
    }

    /**
     * Get all required roles as a simple list
     */
    public List<Role> getRequiredRoles() {
        return roles.stream()
                .map(WorkflowStepRole::getRoleName)
                .distinct()
                .toList();
    }

    /**
     * Add a direct user approver
     */
    public void addApprover(User user) {
        if (user != null && !assignedApprovers.contains(user)) {
            assignedApprovers.add(user);
        }
    }

    /**
     * Remove a direct user approver
     */
    public void removeApprover(User user) {
        assignedApprovers.remove(user);
    }

    /**
     * Check if this step has any approvers (either role-based or direct)
     */
    public boolean hasApprovers() {
        return !roles.isEmpty() || !assignedApprovers.isEmpty();
    }

    /**
     * Get the effective required approvals count (minimum 1)
     */
    public int getEffectiveRequiredApprovals() {
        return (requiredApprovals == null || requiredApprovals <= 0) ? 1 : requiredApprovals;
    }

    // ===== STANDARD GETTERS AND SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkflowTemplate getTemplate() {
        return template;
    }

    public void setTemplate(WorkflowTemplate template) {
        this.template = template;
    }

    public List<WorkflowStepRole> getRoles() {
        return roles;
    }

    public void setRoles(List<WorkflowStepRole> roles) {
        this.roles = roles != null ? roles : new ArrayList<>();
    }

    public List<User> getAssignedApprovers() {
        return assignedApprovers;
    }

    public void setAssignedApprovers(List<User> assignedApprovers) {
        this.assignedApprovers = assignedApprovers != null ? assignedApprovers : new ArrayList<>();
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StepType getStepType() {
        return stepType;
    }

    public void setStepType(StepType stepType) {
        this.stepType = stepType;
    }

    /**
     * ✅ ALIAS: getType() method for compatibility
     */
    public StepType getType() {
        return this.stepType;
    }

    // ✅ ADD: Missing assigneeRole getters and setters
    public Role getAssigneeRole() {
        return assigneeRole;
    }

    public void setAssigneeRole(Role assigneeRole) {
        this.assigneeRole = assigneeRole;
    }

    // ✅ ADD: Missing isRequired getters and setters
    public Boolean getIsRequired() {
        return isRequired != null ? isRequired : true;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    // ✅ ADD: Convenience method for primitive boolean
    public void setIsRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }

    public ApprovalPolicy getApprovalPolicy() {
        return approvalPolicy;
    }

    public void setApprovalPolicy(ApprovalPolicy approvalPolicy) {
        this.approvalPolicy = approvalPolicy;
    }

    public Integer getRequiredApprovals() {
        return requiredApprovals;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public String getAutoApproveCondition() {
        return autoApproveCondition;
    }

    public void setAutoApproveCondition(String autoApproveCondition) {
        this.autoApproveCondition = autoApproveCondition;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // ===== EQUALS, HASHCODE, TOSTRING =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowStep that = (WorkflowStep) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "id=" + id +
                ", stepOrder=" + stepOrder +
                ", name='" + name + '\'' +
                ", stepType=" + stepType +
                ", assigneeRole=" + assigneeRole +
                ", isRequired=" + isRequired +
                ", approvalPolicy=" + approvalPolicy +
                ", requiredApprovals=" + requiredApprovals +
                '}';
    }
}
