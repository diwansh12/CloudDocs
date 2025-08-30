package com.clouddocs.backend.dto.analytics.projections;

import com.clouddocs.backend.entity.WorkflowStatus;

public interface StatusCountProjection {
    WorkflowStatus getStatus();
    long getCnt();
}

