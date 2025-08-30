package com.clouddocs.backend.dto.workflow;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO for workflow history/audit trail
 */
public class WorkflowHistoryDTO {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actionDate;
    
    private String details;
    private String action;
    
    // Performer details
    private Long performedById;
    private String performedByName;

    // Constructors
    public WorkflowHistoryDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getActionDate() { return actionDate; }
    public void setActionDate(LocalDateTime actionDate) { this.actionDate = actionDate; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getPerformedById() { return performedById; }
    public void setPerformedById(Long performedById) { this.performedById = performedById; }

    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }
}
