package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.DocumentDTO;
import com.clouddocs.backend.dto.DocumentUploadRequest;
import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.entity.DocumentShareLink;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.repository.DocumentShareLinkRepository;
import com.clouddocs.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DocumentShareLinkRepository shareLinksRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private AuditService auditService;
    
    // ===== EXISTING METHODS =====
    
    public DocumentDTO uploadDocument(MultipartFile file, DocumentUploadRequest request) {
        try {
            User currentUser = getCurrentUser();
            String storedFileName = fileStorageService.storeFile(file);
            
            Document document = new Document(
                storedFileName,
                file.getOriginalFilename(),
                storedFileName,
                file.getSize(),
                file.getContentType(),
                currentUser
            );
            
            document.setDescription(request.getDescription());
            document.setCategory(request.getCategory());
            document.setTags(request.getTags());
            
            document = documentRepository.save(document);
            auditService.logDocumentUpload(document, currentUser);
            
            return convertToDTO(document);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }
    
    public Page<DocumentDTO> getAllDocuments(int page, int size, String sortBy, String sortDir, 
                                           String search, DocumentStatus status, String category) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Document> documents;
        
        if (search != null && !search.trim().isEmpty()) {
            documents = documentRepository.searchDocuments(search, pageable);
        } else if (status != null || category != null) {
            documents = documentRepository.findWithFilters(null, status, category, null, null, pageable);
        } else {
            documents = documentRepository.findAll(pageable);
        }
        
        return documents.map(this::convertToDTO);
    }
    
    public Page<DocumentDTO> getMyDocuments(int page, int size, String sortBy, String sortDir) {
        User currentUser = getCurrentUser();
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Document> documents = documentRepository.findByUploadedByIdOrderByUploadDateDesc(currentUser.getId(), pageable);
        return documents.map(this::convertToDTO);
    }
    
    public DocumentDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        return convertToDTO(document);
    }
    
    public DocumentDTO updateDocumentStatus(Long id, DocumentStatus status, String rejectionReason) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canChangeDocumentStatus(currentUser)) {
            throw new RuntimeException("You don't have permission to change document status");
        }
        
        DocumentStatus oldStatus = document.getStatus();
        document.setStatus(status);
        
        if (status == DocumentStatus.APPROVED) {
            document.setApprovedBy(currentUser);
            document.setApprovalDate(LocalDateTime.now());
            document.setRejectionReason(null);
        } else if (status == DocumentStatus.REJECTED) {
            document.setRejectionReason(rejectionReason);
            document.setApprovedBy(null);
            document.setApprovalDate(null);
        }
        
        document = documentRepository.save(document);
        auditService.logDocumentStatusChange(document, oldStatus, status, currentUser);
        
        return convertToDTO(document);
    }
    
    public Resource downloadDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        document.incrementDownloadCount();
        documentRepository.save(document);
        
        User currentUser = getCurrentUser();
        auditService.logDocumentDownload(document, currentUser);
        
        return fileStorageService.loadFileAsResource(document.getFilePath());
    }
    
    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canDeleteDocument(document, currentUser)) {
            throw new RuntimeException("You don't have permission to delete this document");
        }
        
        fileStorageService.deleteFile(document.getFilePath());
        auditService.logDocumentDeletion(document, currentUser);
        documentRepository.delete(document);
    }
    
    public DocumentDTO updateDocument(Long id, DocumentUploadRequest request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canUpdateDocument(document, currentUser)) {
            throw new RuntimeException("You don't have permission to update this document");
        }
        
        document.setDescription(request.getDescription());
        document.setCategory(request.getCategory());
        document.setTags(request.getTags());
        document.setLastModified(LocalDateTime.now());
        
        document = documentRepository.save(document);
        auditService.logDocumentUpdate(document, currentUser);
        
        return convertToDTO(document);
    }
    
    public List<String> getAllCategories() {
        return documentRepository.findAllCategories();
    }
    
    public List<String> getAllTags() {
        return documentRepository.findAllTags();
    }
    
    public Page<DocumentDTO> getPendingDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").ascending());
        Page<Document> documents = documentRepository.findPendingDocuments(pageable);
        return documents.map(this::convertToDTO);
    }
    
    // ===== NEW METHODS FOR SHARE AND METADATA =====
    
    /**
     * ✅ NEW: Update document metadata
     */
    public DocumentDTO updateDocumentMetadata(Long id, Map<String, Object> metadata) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canUpdateDocument(document, currentUser)) {
            throw new RuntimeException("You don't have permission to update this document");
        }
        
        // Update metadata fields
        if (metadata.containsKey("title")) {
            document.setOriginalFilename((String) metadata.get("title"));
        }
        if (metadata.containsKey("description")) {
            document.setDescription((String) metadata.get("description"));
        }
        if (metadata.containsKey("category")) {
            document.setCategory((String) metadata.get("category"));
        }
        if (metadata.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) metadata.get("tags");
            document.setTags(tags);
        }
        
        document.setLastModified(LocalDateTime.now());
        document = documentRepository.save(document);
        
        auditService.logDocumentUpdate(document, currentUser);
        
        return convertToDTO(document);
    }
    
    /**
     * ✅ NEW: Generate share link
     */
    public Map<String, Object> generateShareLink(Long id, Map<String, Object> options) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canShareDocument(document, currentUser)) {
            throw new RuntimeException("You don't have permission to share this document");
        }
        
        // Extract options
        Integer expiryHours = (Integer) options.getOrDefault("expiryHours", 24);
        Boolean allowDownload = (Boolean) options.getOrDefault("allowDownload", true);
        String password = (String) options.get("password");
        
        // Generate unique share ID
        String shareId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);
        
        // Create share link entity
        DocumentShareLink shareLink = new DocumentShareLink();
        shareLink.setDocument(document);
        shareLink.setShareId(shareId);
        shareLink.setCreatedBy(currentUser);
        shareLink.setCreatedAt(LocalDateTime.now());
        shareLink.setExpiresAt(expiresAt);
        shareLink.setAllowDownload(allowDownload);
        shareLink.setPassword(password);
        shareLink.setAccessCount(0);
        shareLink.setActive(true);
        
        shareLink = shareLinksRepository.save(shareLink);
        
        // Generate public URL
        String baseUrl = "https://cloud-docs-tan.vercel.app/"; // Configure this
        String shareUrl = baseUrl + "/shared/" + shareId;
        
        Map<String, Object> response = new HashMap<>();
        response.put("shareUrl", shareUrl);
        response.put("shareId", shareId);
        response.put("expiresAt", expiresAt.toString());
        
        auditService.logDocumentShared(document, currentUser, shareId);
        
        return response;
    }
    
    /**
     * ✅ NEW: Get existing share links
     */
    public List<Map<String, Object>> getShareLinks(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));
        
        User currentUser = getCurrentUser();
        
        if (!canViewShareLinks(document, currentUser)) {
            throw new RuntimeException("You don't have permission to view share links for this document");
        }
        
        List<DocumentShareLink> shareLinks = shareLinksRepository.findByDocumentAndActiveTrue(document);
        
        return shareLinks.stream().map(link -> {
            Map<String, Object> linkMap = new HashMap<>();
            linkMap.put("id", link.getShareId());
            linkMap.put("url", "https://cloud-docs-tan.vercel.app/shared/" + link.getShareId());
            linkMap.put("expiresAt", link.getExpiresAt().toString());
            linkMap.put("allowDownload", link.getAllowDownload());
            linkMap.put("hasPassword", link.getPassword() != null);
            linkMap.put("createdAt", link.getCreatedAt().toString());
            linkMap.put("accessCount", link.getAccessCount());
            return linkMap;
        }).collect(Collectors.toList());
    }
    
    /**
     * ✅ NEW: Revoke share link
     */
    public void revokeShareLink(Long documentId, String shareId) {
        DocumentShareLink shareLink = shareLinksRepository.findByShareIdAndActiveTrue(shareId)
                .orElseThrow(() -> new RuntimeException("Share link not found or already revoked"));
        
        User currentUser = getCurrentUser();
        
        if (!canRevokeShareLink(shareLink, currentUser)) {
            throw new RuntimeException("You don't have permission to revoke this share link");
        }
        
        shareLink.setActive(false);
        shareLink.setRevokedAt(LocalDateTime.now());
        shareLink.setRevokedBy(currentUser);
        shareLinksRepository.save(shareLink);
        
        auditService.logShareLinkRevoked(shareLink.getDocument(), currentUser, shareId);
    }
    
    /**
     * ✅ NEW: Access shared document (public)
     */
    public DocumentDTO accessSharedDocument(String shareId, String password) {
        DocumentShareLink shareLink = shareLinksRepository.findByShareIdAndActiveTrue(shareId)
                .orElseThrow(() -> new RuntimeException("Share link not found or expired"));
        
        // Check if link is expired
        if (shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link has expired");
        }
        
        // Check password if required
        if (shareLink.getPassword() != null && !shareLink.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        
        // Increment access count
        shareLink.setAccessCount(shareLink.getAccessCount() + 1);
        shareLink.setLastAccessedAt(LocalDateTime.now());
        shareLinksRepository.save(shareLink);
        
        return convertToDTO(shareLink.getDocument());
    }
    
    /**
     * ✅ NEW: Download shared document (public)
     */
    public Resource downloadSharedDocument(String shareId, String password) {
        DocumentShareLink shareLink = shareLinksRepository.findByShareIdAndActiveTrue(shareId)
                .orElseThrow(() -> new RuntimeException("Share link not found or expired"));
        
        // Check if link is expired
        if (shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link has expired");
        }
        
        // Check if download is allowed
        if (!shareLink.getAllowDownload()) {
            throw new RuntimeException("Download not allowed for this share link");
        }
        
        // Check password if required
        if (shareLink.getPassword() != null && !shareLink.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        
        // Increment access count
        shareLink.setAccessCount(shareLink.getAccessCount() + 1);
        shareLink.setLastAccessedAt(LocalDateTime.now());
        shareLinksRepository.save(shareLink);
        
        return fileStorageService.loadFileAsResource(shareLink.getDocument().getFilePath());
    }
    
    /**
     * ✅ NEW: Get shared document info (public)
     */
    public DocumentDTO getSharedDocumentInfo(String shareId, String password) {
        return accessSharedDocument(shareId, password);
    }
    
    /**
     * ✅ NEW: Get document statistics
     */
    public Map<String, Object> getDocumentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long totalDocuments = documentRepository.count();
        stats.put("total", totalDocuments);
        
        // Count by status
        Map<String, Long> byStatus = new HashMap<>();
        for (DocumentStatus status : DocumentStatus.values()) {
            long count = documentRepository.countByStatus(status);
            byStatus.put(status.name().toLowerCase(), count);
        }
        stats.put("byStatus", byStatus);
        
        // Count by category
         Map<String, Long> byCategory = new HashMap<>();
    List<Object[]> categoryStats = documentRepository.countByCategory();
    for (Object[] row : categoryStats) {
        String category = (String) row[0];
        Long count = (Long) row[1];
        byCategory.put(category, count);
    }
    stats.put("byCategory", byCategory);
    
    // Recent uploads (last 7 days)
    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
    long recentUploads = documentRepository.countByUploadDateAfter(weekAgo);
    stats.put("recentUploads", recentUploads);
    
    // Total downloads - ✅ NOW THIS WILL WORK
    long totalDownloads = documentRepository.sumDownloadCounts();
    stats.put("totalDownloads", totalDownloads);
    
    return stats;
}
    
    // ===== HELPER METHODS =====
    
    private DocumentDTO convertToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setFilename(document.getFilename());
        dto.setOriginalFilename(document.getOriginalFilename());
        dto.setDescription(document.getDescription());
        dto.setFileSize(document.getFileSize());
        dto.setFormattedFileSize(document.getFormattedFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setStatus(document.getStatus());
        dto.setVersionNumber(document.getVersionNumber());
        dto.setUploadedByName(document.getUploadedBy().getFullName());
        dto.setUploadedById(document.getUploadedBy().getId());
        dto.setUploadDate(document.getUploadDate());
        dto.setLastModified(document.getLastModified());
        dto.setDownloadCount(document.getDownloadCount());
        dto.setTags(document.getTags());
        dto.setCategory(document.getCategory());
        dto.setDocumentType(document.getDocumentType());
        
        if (document.getApprovedBy() != null) {
            dto.setApprovedByName(document.getApprovedBy().getFullName());
            dto.setApprovalDate(document.getApprovalDate());
        }
        
        dto.setRejectionReason(document.getRejectionReason());
        
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    private boolean canChangeDocumentStatus(User user) {
        return user.getRole().name().equals("ADMIN") || user.getRole().name().equals("MANAGER");
    }
    
    private boolean canDeleteDocument(Document document, User user) {
        return user.getRole().name().equals("ADMIN") || 
               document.getUploadedBy().getId().equals(user.getId());
    }
    
    private boolean canUpdateDocument(Document document, User user) {
        return user.getRole().name().equals("ADMIN") || 
               user.getRole().name().equals("MANAGER") ||
               document.getUploadedBy().getId().equals(user.getId());
    }
    
    // ✅ NEW: Helper methods for share permissions
    private boolean canShareDocument(Document document, User user) {
        return user.getRole().name().equals("ADMIN") || 
               user.getRole().name().equals("MANAGER") ||
               document.getUploadedBy().getId().equals(user.getId());
    }
    
    private boolean canViewShareLinks(Document document, User user) {
        return canShareDocument(document, user);
    }
    
    private boolean canRevokeShareLink(DocumentShareLink shareLink, User user) {
        return user.getRole().name().equals("ADMIN") || 
               shareLink.getCreatedBy().getId().equals(user.getId()) ||
               shareLink.getDocument().getUploadedBy().getId().equals(user.getId());
    }
}
