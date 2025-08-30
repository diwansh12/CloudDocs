package com.clouddocs.backend.dto.analytics.projections;

public interface StepMetricsProjection {
    Integer getStepOrder();
    Double getAvgTaskCompletionHours();
    Long getApprovals();
    Long getRejections();
    
    // Add missing methods
    Long getTotalTasks();
    Long getCompletedTasks();
    Long getPendingTasks();
    Long getOverdueTasks();
}

