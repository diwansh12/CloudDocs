package com.clouddocs.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateWorkflowRequest {
    private Long documentId;
    private UUID templateId;
    private String title;
    private String description;
    private String priority;
    
    public CreateWorkflowRequest() {}
    
    public CreateWorkflowRequest(Long documentId, UUID templateId, String title, String description, String priority) {
        this.documentId = documentId;
        this.templateId = templateId;
        this.title = title;
        this.description = description;
        this.priority = priority;
    }
}
