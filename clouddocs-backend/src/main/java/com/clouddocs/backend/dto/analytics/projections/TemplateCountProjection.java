package com.clouddocs.backend.dto.analytics.projections;

public interface TemplateCountProjection {
    String getTemplateId();
    String getTemplateName();
    Long getTotal();
    Long getApproved();
    Long getRejected();
    Double getAvgDurationHours();
}
