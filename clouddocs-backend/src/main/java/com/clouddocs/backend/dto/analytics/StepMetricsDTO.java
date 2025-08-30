package com.clouddocs.backend.dto.analytics;

public class StepMetricsDTO {
    public Integer stepOrder;
    public Double avgTaskCompletionHours;
    public Long approvals;
    public Long rejections;
    
    // New fields that were missing
    public Long totalTasks;
    public Long completedTasks;
    public Long pendingTasks;
    public Long overdueTasks;
    public Double completionRate;
}
