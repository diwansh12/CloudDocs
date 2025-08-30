package com.clouddocs.backend.entity;

public enum WorkflowType {
    DOCUMENT_APPROVAL("Document Approval"),
    DOCUMENT_REVIEW("Document Review"),
    CHANGE_REQUEST("Change Request"),
    CUSTOM("Custom Workflow");
    
    private final String displayName;
    
    WorkflowType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
