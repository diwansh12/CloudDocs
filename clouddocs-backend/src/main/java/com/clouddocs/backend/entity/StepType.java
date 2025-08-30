package com.clouddocs.backend.entity;

public enum StepType {
    APPROVAL,           // For approval steps
    REVIEW,            // For review steps
    NOTIFICATION,      // For notification/alert steps
    VALIDATION,        // For data validation steps
    DATA_PROCESSING,   // For data transformation/processing
    CUSTOM_ACTION      // For custom/user-defined actions
}