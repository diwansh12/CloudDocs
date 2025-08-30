package com.clouddocs.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing workflow history/audit trail
 */
@Entity
@Table(name = "workflow_history")
public class WorkflowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "details", length = 2000)
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate;

    // Constructors
    public WorkflowHistory() {
        this.actionDate = LocalDateTime.now();
    }

    public WorkflowHistory(WorkflowInstance instance, String action, String details, User performedBy) {
        this();
        this.workflowInstance = instance;
        this.action = action;
        this.details = details;
        this.performedBy = performedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getActionDate() { return actionDate; }
    public void setActionDate(LocalDateTime actionDate) { this.actionDate = actionDate; }
}
