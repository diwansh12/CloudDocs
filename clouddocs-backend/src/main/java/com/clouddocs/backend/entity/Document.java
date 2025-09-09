package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "documents")
public class Document {
    
    private static final Logger logger = LoggerFactory.getLogger(Document.class);
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    private String description;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type")
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PENDING;
    
    @Column(name = "version_number")
    private Integer versionNumber = 1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    @JsonIgnore  // ✅ CRITICAL: Prevents Jackson serialization issues
    private User uploadedBy;
    
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Column(name = "download_count")
    private Integer downloadCount = 0;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tags")
    @JsonIgnore  // ✅ CRITICAL: Prevents lazy collection serialization issues
    private List<String> tags = new ArrayList<>();
    
    private String category;
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "approved_by")
    @JsonIgnore  // ✅ CRITICAL: Prevents Jackson serialization issues
    private User approvedBy;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "document_type")
    private String documentType;

     @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding; // Store as JSON string
    
    @Column(name = "embedding_generated")
    private Boolean embeddingGenerated = false;
    
    // Constructors
    public Document() {
        this.uploadDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }
    
    public Document(String filename, String originalFilename, String filePath, 
                   Long fileSize, String mimeType, User uploadedBy) {
        this();
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadedBy = uploadedBy;
        this.documentType = determineDocumentType(mimeType);
    }
    
    private String determineDocumentType(String mimeType) {
        if (mimeType == null) return "unknown";
        
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.contains("pdf")) return "pdf";
        if (mimeType.contains("word") || mimeType.contains("document")) return "document";
        if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) return "spreadsheet";
        if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) return "presentation";
        if (mimeType.startsWith("text/")) return "text";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        
        return "file";
    }
    
    // ✅ SAFE ACCESSOR METHODS - Prevent LazyInitializationException
    public String getUploadedByNameSafe() {
        try {
            return uploadedBy != null ? uploadedBy.getFullName() : "Unknown";
        } catch (Exception e) {
            logger.warn("Could not access uploadedBy for document {}: {}", id, e.getMessage());
            return "Unknown";
        }
    }

    public Long getUploadedByIdSafe() {
        try {
            return uploadedBy != null ? uploadedBy.getId() : null;
        } catch (Exception e) {
            logger.warn("Could not access uploadedBy ID for document {}: {}", id, e.getMessage());
            return null;
        }
    }
    
    public String getApprovedByNameSafe() {
        try {
            return approvedBy != null ? approvedBy.getFullName() : null;
        } catch (Exception e) {
            logger.warn("Could not access approvedBy for document {}: {}", id, e.getMessage());
            return null;
        }
    }

    public List<String> getTagsSafe() {
        try {
            return tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        } catch (Exception e) {
            logger.warn("Could not access tags for document {}: {}", id, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Standard Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { 
        this.status = status; 
        this.lastModified = LocalDateTime.now();
    }
    
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    
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
    
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    
    // Utility methods
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null) ? 1 : this.downloadCount + 1;
    }
    
    public String getFormattedFileSize() {
        if (fileSize == null) return "Unknown";
        
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
    }

     public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    
    public Boolean getEmbeddingGenerated() { return embeddingGenerated; }
    public void setEmbeddingGenerated(Boolean embeddingGenerated) { this.embeddingGenerated = embeddingGenerated; }
}
