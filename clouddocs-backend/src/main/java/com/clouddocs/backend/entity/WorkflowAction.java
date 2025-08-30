package com.clouddocs.backend.entity;

public enum WorkflowAction {
    CREATED,           // Workflow instance created
    STARTED,           // Workflow started
    STEP_COMPLETED,    // Workflow step completed
    TASK_ASSIGNED,          // Task assigned to user
    REASSIGNED,        // Task reassigned to different user
    COMMENT_ADDED,     // Comment added to workflow
    STATUS_CHANGED,    // Workflow status changed
    APPROVED,          // Workflow approved
    REJECTED,          // Workflow rejected
    CANCELLED,         // Workflow cancelled
    ON_HOLD,          // Workflow put on hold
    RESUMED            // Workflow resumed from hold
}