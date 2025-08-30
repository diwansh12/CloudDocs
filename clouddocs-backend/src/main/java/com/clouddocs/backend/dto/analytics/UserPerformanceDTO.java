package com.clouddocs.backend.dto.analytics;

public class UserPerformanceDTO {
    public Long userId;
    public String username;
    public Long totalTasks;
    public Long completedTasks;
    public Long approvals;
    public Long rejections;
    public Double avgCompletionHours;
    public Double completionRate;
}
