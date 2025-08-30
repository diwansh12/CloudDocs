package com.clouddocs.backend.dto.analytics;

public class TemplateMetricsDTO {
    public String templateId;
    public String templateName;
    public Long total;
    public Long approved;
    public Long rejected;
    public Double avgDurationHours;
    
    // New field that was missing
    public Double approvalRate;
}
