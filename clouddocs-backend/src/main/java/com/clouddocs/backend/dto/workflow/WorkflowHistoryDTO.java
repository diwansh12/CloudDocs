package com.clouddocs.backend.dto.workflow;

import java.time.OffsetDateTime; // ✅ CHANGED: Use OffsetDateTime instead of LocalDateTime

/**
 * DTO for workflow history/audit trail
 */
public class WorkflowHistoryDTO {
    
    private Long id;
    
    // ✅ CHANGED: Use OffsetDateTime for proper timezone handling
    // ✅ REMOVED: @JsonFormat annotation - not needed since OffsetDateTime serializes correctly
    private OffsetDateTime actionDate;
    
    private String details;
    private String action;
    
    // Performer details
    private Long performedById;
    private String performedByName;

    // Constructors
    public WorkflowHistoryDTO() {}

    // ✅ UPDATED: Getters and Setters for OffsetDateTime
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OffsetDateTime getActionDate() { return actionDate; }
    public void setActionDate(OffsetDateTime actionDate) { this.actionDate = actionDate; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getPerformedById() { return performedById; }
    public void setPerformedById(Long performedById) { this.performedById = performedById; }

    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }
}
