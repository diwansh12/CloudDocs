package com.clouddocs.backend.entity;

public enum WorkflowStatus {
    COMPLETED,
    PENDING,          // Workflow is created but not started
    IN_PROGRESS,    // Workflow is currently active
    ON_HOLD,        // Workflow is temporarily paused
    APPROVED,       // Workflow completed with approval
    REJECTED,       // Workflow completed with rejection
    CANCELLED,      // Workflow was cancelled before completion
    EXPIRED         // Workflow expired/timed out
}