package com.clouddocs.backend.dto.analytics;

public class OverviewMetricsDTO {
    public Long total;
    public Long approved;
    public Long rejected;
    public Long inProgress;
    public Long cancelled;
    public Long overdueTasks;
    public Double avgApprovalHours;
    public Double avgTaskCompletionHours;
    
    // New fields that were missing
    public Long totalTasksInPeriod;
    public Long completedTasksInPeriod;
    public Double completionRate;
}
