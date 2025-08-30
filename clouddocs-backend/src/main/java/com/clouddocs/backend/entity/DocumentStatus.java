package com.clouddocs.backend.entity;

public enum DocumentStatus {
    DRAFT("Draft", "#9ca3af", "Document is in draft state"),
    PENDING("Pending", "#f59e0b", "Document is awaiting approval"),
    APPROVED("Approved", "#10b981", "Document has been approved"),
    REJECTED("Rejected", "#ef4444", "Document has been rejected"),
    IN_REVIEW("In Review", "#800080", "Document is in review");  // ✅ Fixed syntax

    private final String displayName;
    private final String color;
    private final String description;
    
    DocumentStatus(String displayName, String color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
}
