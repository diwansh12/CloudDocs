package com.clouddocs.backend.dto.analytics.projections;

public interface TemplateCountProjection {
    String getTemplateId();
    String getTemplateName();
    long getTotal();
    long getApproved();
    long getRejected();
    Double getAvgDurationHours();
}
