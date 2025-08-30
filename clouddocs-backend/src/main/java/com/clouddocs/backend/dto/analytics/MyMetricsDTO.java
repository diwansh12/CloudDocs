package com.clouddocs.backend.dto.analytics;

public class MyMetricsDTO {
    public Long myInitiatedTotal;
    public Long myInitiatedApproved;
    public Long myInitiatedRejected;
    public Long myPendingTasks;
    public Long myCompletedTasks;
    public Double myAvgTaskCompletionHours;
    
    // New field that was missing
    public Double myTaskCompletionRate;
}
