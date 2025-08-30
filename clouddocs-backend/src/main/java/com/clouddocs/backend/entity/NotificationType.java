package com.clouddocs.backend.entity;

public enum NotificationType {
    GENERAL("General Notification"),
    TASK_ASSIGNED("Task Assigned"),
    TASK_COMPLETED("Task Completed"), 
    TASK_OVERDUE("Task Overdue"),
    WORKFLOW_APPROVED("Workflow Approved"),
    WORKFLOW_REJECTED("Workflow Rejected"),
    WORKFLOW_CANCELLED("Workflow Cancelled"),
    WORKFLOW_OVERDUE("Workflow Overdue"),
    SYSTEM_ALERT("System Alert"),
    REMINDER("Reminder");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
