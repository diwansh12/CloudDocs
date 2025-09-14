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
    @OneToMany(mappedBy = "step", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false, fetch = FetchType.LAZY)
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

    // ✅ FIXED: Proper entity relationship mapping (not @Enumerated)
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
     * ✅ FIXED: Add a role-based approver to this step (uses proper constructor)
     */
    public void addRole(Role roleEntity) {
    if (roleEntity == null) return;
    
    // ✅ FIXED: Compare ERole enums properly
    boolean exists = roles.stream()
        .anyMatch(stepRole -> stepRole.getRoleName().equals(roleEntity.getName()));
    
    if (!exists) {
      WorkflowStepRole stepRole = new WorkflowStepRole(this, roleEntity);
this.roles.add(stepRole); // ✅ Actually add it to the list

    }
}

    /**
     * ✅ FIXED: Remove a role from this step (corrected enum comparison)
     */
   public void removeRole(Role roleEntity) {
    if (roleEntity == null) return;
    
    // ✅ FIXED: Compare ERole enums properly
    roles.removeIf(stepRole -> stepRole.getRoleName().equals(roleEntity.getName()));
}


    /**
     * ✅ ENHANCED: Get all required roles as Role entities (placeholder for service layer conversion)
     */
    public List<Role> getRequiredRoles() {
        // This method would need RoleRepository to convert enums back to entities
        // For now, return empty list and handle conversion in service layer
        return new ArrayList<>();
    }

    /**
     * ✅ FIXED: Get required role enums directly - corrected return type
     */
  public List<ERole> getRequiredRoleEnums() {
   return roles.stream()
    .map(WorkflowStepRole::getRoleName) // Returns Role entity
    .filter(Objects::nonNull)
    .map(Role::getName) // Convert Role → ERole
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

    /**
     * ✅ FIXED: Check if step has specific role requirement - corrected enum comparison
     */
  public boolean hasRole(ERole roleEnum) {
   return roles.stream()
    .map(WorkflowStepRole::getRoleName) // ✅ Step 1: WorkflowStepRole → Role
    .filter(Objects::nonNull)           // ✅ Step 2: Filter nulls
    .map(Role::getName)                 // ✅ Step 3: Role → ERole  
    .anyMatch(eRole -> eRole == roleEnum); // ✅ Step 4: Compare ERole == ERole


}

    /**
     * ✅ NEW: Get total number of role-based approvers
     */
    public int getRoleBasedApproverCount() {
        return roles.size();
    }

    /**
     * ✅ NEW: Get total number of direct approvers
     */
    public int getDirectApproverCount() {
        return assignedApprovers.size();
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
     * Alias: getType() method for compatibility
     */
    public StepType getType() {
        return this.stepType;
    }

    public Role getAssigneeRole() {
        return assigneeRole;
    }

    public void setAssigneeRole(Role assigneeRole) {
        this.assigneeRole = assigneeRole;
    }

    public Boolean getIsRequired() {
        return isRequired != null ? isRequired : true;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

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
                ", assigneeRole=" + (assigneeRole != null ? assigneeRole.getName() : null) +
                ", isRequired=" + isRequired +
                ", approvalPolicy=" + approvalPolicy +
                ", requiredApprovals=" + requiredApprovals +
                ", rolesCount=" + roles.size() +
                ", directApproversCount=" + assignedApprovers.size() +
                '}';
    }
}



