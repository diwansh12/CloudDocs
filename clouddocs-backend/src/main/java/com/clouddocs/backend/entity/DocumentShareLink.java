package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a shared link for document access
 */
@Entity
@Table(name = "document_share_links", indexes = {
    @Index(name = "idx_share_id", columnList = "share_id"),
    @Index(name = "idx_document_active", columnList = "document_id, active"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
public class DocumentShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_id", unique = true, nullable = false, length = 36)
    private String shareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "allow_download", nullable = false)
    private Boolean allowDownload = true;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "max_access_count")
    private Integer maxAccessCount;

    // Constructors
    public DocumentShareLink() {}

    public DocumentShareLink(String shareId, Document document, User createdBy) {
        this.shareId = shareId;
        this.document = document;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.allowDownload = true;
        this.accessCount = 0;
        this.active = true;
    }

    public DocumentShareLink(String shareId, Document document, User createdBy, 
                           LocalDateTime expiresAt, Boolean allowDownload, String password) {
        this(shareId, document, createdBy);
        this.expiresAt = expiresAt;
        this.allowDownload = allowDownload;
        this.password = password;
    }

    // Business Logic Methods
    
    /**
     * Check if the share link is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if the share link is valid for access
     */
    public boolean isValidForAccess() {
        return active && !isExpired() && 
               (maxAccessCount == null || accessCount < maxAccessCount);
    }

    /**
     * Check if password is required and valid
     */
    public boolean isPasswordValid(String providedPassword) {
        if (password == null) return true;
        return Objects.equals(password, providedPassword);
    }

    /**
     * Increment access count
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Revoke the share link
     */
    public void revoke(User revokedBy) {
        this.active = false;
        this.revokedBy = revokedBy;
        this.revokedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public User getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(User revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Boolean getAllowDownload() {
        return allowDownload;
    }

    public void setAllowDownload(Boolean allowDownload) {
        this.allowDownload = allowDownload;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getMaxAccessCount() {
        return maxAccessCount;
    }

    public void setMaxAccessCount(Integer maxAccessCount) {
        this.maxAccessCount = maxAccessCount;
    }

    // Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentShareLink that = (DocumentShareLink) o;
        return Objects.equals(shareId, that.shareId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shareId);
    }

    // ToString

    @Override
    public String toString() {
        return "DocumentShareLink{" +
                "id=" + id +
                ", shareId='" + shareId + '\'' +
                ", documentId=" + (document != null ? document.getId() : null) +
                ", createdBy=" + (createdBy != null ? createdBy.getUsername() : null) +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", active=" + active +
                ", accessCount=" + accessCount +
                '}';
    }
}
