package com.clouddocs.backend.dto;

import java.util.List;

public class DocumentUploadRequest {
    private String description;
    private String category;
    private List<String> tags;
    
    // Constructors
    public DocumentUploadRequest() {}
    
    public DocumentUploadRequest(String description, String category, List<String> tags) {
        this.description = description;
        this.category = category;
        this.tags = tags;
    }
    
    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}

