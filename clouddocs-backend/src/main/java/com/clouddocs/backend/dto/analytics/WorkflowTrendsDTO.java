package com.clouddocs.backend.dto.analytics;

import com.clouddocs.backend.entity.WorkflowStatus;
import java.time.LocalDate;
import java.util.Map;

public class WorkflowTrendsDTO {
    public Map<LocalDate, Long> dailyCreations;
    public Map<LocalDate, Long> dailyCompletions;
    public Map<WorkflowStatus, Long> statusTrends;
}
