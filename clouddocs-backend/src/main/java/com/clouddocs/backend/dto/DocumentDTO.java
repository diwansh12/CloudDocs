package com.clouddocs.backend.dto;

import com.clouddocs.backend.entity.DocumentStatus;
import java.time.LocalDateTime;
import java.util.List;

public class DocumentDTO {
    private Long id;
    private String filename;
    private String originalFilename;
    private String description;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private DocumentStatus status;
    private Integer versionNumber;
    private String uploadedByName;
    private Long uploadedById;
    private LocalDateTime uploadDate;
    private LocalDateTime lastModified;
    private Integer downloadCount;
    private List<String> tags;
    private String category;
    private String documentType;
    private String approvedByName;
    private LocalDateTime approvalDate;
    private String rejectionReason;
    private Double aiScore;
     private String searchType;
    
    // Constructors
    public DocumentDTO() {}
    
    // Full constructor
    public DocumentDTO(Long id, String filename, String originalFilename, String description,
                      Long fileSize, String formattedFileSize, String mimeType, DocumentStatus status,
                      Integer versionNumber, String uploadedByName, Long uploadedById,
                      LocalDateTime uploadDate, LocalDateTime lastModified, Integer downloadCount,
                      List<String> tags, String category, String documentType) {
        this.id = id;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.description = description;
        this.fileSize = fileSize;
        this.formattedFileSize = formattedFileSize;
        this.mimeType = mimeType;
        this.status = status;
        this.versionNumber = versionNumber;
        this.uploadedByName = uploadedByName;
        this.uploadedById = uploadedById;
        this.uploadDate = uploadDate;
        this.lastModified = lastModified;
        this.downloadCount = downloadCount;
        this.tags = tags;
        this.category = category;
        this.documentType = documentType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getFormattedFileSize() { return formattedFileSize; }
    public void setFormattedFileSize(String formattedFileSize) { this.formattedFileSize = formattedFileSize; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    
    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }
    
    public Long getUploadedById() { return uploadedById; }
    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }
    
    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    
    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
    
    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

     public Double getAiScore() { 
        return aiScore; 
    }
    
    public void setAiScore(Double aiScore) { 
        this.aiScore = aiScore; 
    }

      public String getSearchType() {
        return searchType;
    }
    
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
}
