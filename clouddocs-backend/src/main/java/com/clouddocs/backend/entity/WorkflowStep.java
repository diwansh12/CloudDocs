package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
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
     * ✅ FIXED: Changed List to Set to avoid MultipleBagFetchException
     * Role-based approvers for this step
     */
    @OneToMany(mappedBy = "step", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false, fetch = FetchType.LAZY)
    private Set<WorkflowStepRole> roles = new HashSet<>();

    /**
     * ✅ FIXED: Changed List to Set to avoid MultipleBagFetchException
     * Direct user assignments as approvers (alternative to role-based)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "workflow_step_approvers",
        joinColumns = @JoinColumn(name = "step_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedApprovers = new HashSet<>();

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee_role_id")
    private Role assigneeRole;

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
     * ✅ UPDATED: Add a role-based approver to this step
     */
    public void addRole(Role roleEntity) {
        if (roleEntity == null) return;
        
        boolean exists = roles.stream()
            .anyMatch(stepRole -> stepRole.getRoleName().equals(roleEntity.getName()));
        
        if (!exists) {
            WorkflowStepRole stepRole = new WorkflowStepRole(this, roleEntity.getName());
            this.roles.add(stepRole);
        }
    }

    /**
     * ✅ UPDATED: Remove a role from this step
     */
    public void removeRole(Role roleEntity) {
        if (roleEntity == null) return;
        
        roles.removeIf(stepRole -> stepRole.getRoleName().equals(roleEntity.getName()));
    }

    /**
     * ✅ ENHANCED: Get all required roles as Role entities
     */
    public Set<Role> getRequiredRoles() {
        // This method would need RoleRepository to convert enums back to entities
        // For now, return empty set and handle conversion in service layer
        return new HashSet<>();
    }

    /**
     * ✅ UPDATED: Get required role enums directly
     */
    public Set<ERole> getRequiredRoleEnums() {
        return roles.stream()
            .map(WorkflowStepRole::getRoleName)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * ✅ UPDATED: Add a direct user approver
     */
    public void addApprover(User user) {
        if (user != null) {
            assignedApprovers.add(user);
        }
    }

    /**
     * ✅ UPDATED: Remove a direct user approver
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

    /**
     * ✅ UPDATED: Check if step has specific role requirement
     */
    public boolean hasRole(ERole roleEnum) {
        return roles.stream()
            .map(WorkflowStepRole::getRoleName)
            .filter(Objects::nonNull)
            .anyMatch(eRole -> eRole == roleEnum);
    }

    /**
     * Get total number of role-based approvers
     */
    public int getRoleBasedApproverCount() {
        return roles.size();
    }

    /**
     * Get total number of direct approvers
     */
    public int getDirectApproverCount() {
        return assignedApprovers.size();
    }

    // ===== GETTERS AND SETTERS =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowTemplate getTemplate() { return template; }
    public void setTemplate(WorkflowTemplate template) { this.template = template; }

    // ✅ UPDATED: Changed return type from List to Set
    public Set<WorkflowStepRole> getRoles() { return roles; }
    public void setRoles(Set<WorkflowStepRole> roles) { 
        this.roles = roles != null ? roles : new HashSet<>(); 
    }

    // ✅ UPDATED: Changed return type from List to Set
    public Set<User> getAssignedApprovers() { return assignedApprovers; }
    public void setAssignedApprovers(Set<User> assignedApprovers) { 
        this.assignedApprovers = assignedApprovers != null ? assignedApprovers : new HashSet<>(); 
    }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }

    public StepType getType() { return this.stepType; }

    public Role getAssigneeRole() { return assigneeRole; }
    public void setAssigneeRole(Role assigneeRole) { this.assigneeRole = assigneeRole; }

    public Boolean getIsRequired() { return isRequired != null ? isRequired : true; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    public void setIsRequired(boolean isRequired) { this.isRequired = isRequired; }

    public ApprovalPolicy getApprovalPolicy() { return approvalPolicy; }
    public void setApprovalPolicy(ApprovalPolicy approvalPolicy) { this.approvalPolicy = approvalPolicy; }

    public Integer getRequiredApprovals() { return requiredApprovals; }
    public void setRequiredApprovals(Integer requiredApprovals) { this.requiredApprovals = requiredApprovals; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }

    public String getAutoApproveCondition() { return autoApproveCondition; }
    public void setAutoApproveCondition(String autoApproveCondition) { this.autoApproveCondition = autoApproveCondition; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // ===== EQUALS, HASHCODE, TOSTRING =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowStep)) return false;
        WorkflowStep that = (WorkflowStep) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "id=" + id +
                ", stepOrder=" + stepOrder +
                ", name='" + name + '\'' +
                ", stepType=" + stepType +
                ", assigneeRole=" + (assigneeRole != null ? assigneeRole.getName() : null) +
                ", isRequired=" + isRequired +
                ", approvalPolicy=" + approvalPolicy +
                ", requiredApprovals=" + requiredApprovals +
                ", rolesCount=" + roles.size() +
                ", directApproversCount=" + assignedApprovers.size() +
                '}';
    }
}
