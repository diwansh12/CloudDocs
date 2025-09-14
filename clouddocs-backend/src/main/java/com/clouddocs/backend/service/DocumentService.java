package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.DocumentDTO;
import com.clouddocs.backend.dto.DocumentUploadRequest;
import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.entity.ERole;
import com.clouddocs.backend.entity.DocumentShareLink;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.repository.DocumentShareLinkRepository;
import com.clouddocs.backend.repository.UserRepository;

import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    
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

    // ===== EXISTING METHODS (UNCHANGED) =====
    
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
    
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getAllDocuments(int page, int size, String sortBy, String sortDir, 
                                           String search, DocumentStatus status, String category) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Document> documents;
            
            if (search != null && !search.trim().isEmpty()) {
                documents = documentRepository.searchDocumentsWithTags(search, pageable);
            } else if (status != null || category != null) {
                documents = documentRepository.findWithFilters(search, status, category, null, null, pageable);
            } else {
                documents = documentRepository.findAllWithTagsAndUsers(pageable);
            }
            
            return documents.map(this::convertToDTO);
            
        } catch (Exception e) {
            log.error("❌ Error in getAllDocuments: {}", e.getMessage(), e);
            return Page.empty(PageRequest.of(page, size));
        }
    }

    public Page<DocumentDTO> getMyDocuments(int page, int size, String sortBy, String sortDir) {
        User currentUser = getCurrentUser();
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Document> documents = documentRepository.findByUploadedByIdWithTagsAndUsers(currentUser.getId(), pageable);
        return documents.map(this::convertToDTO);
    }
        
    public DocumentDTO getDocumentById(Long id) {
        Document document = documentRepository.findByIdWithTags(id)
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
    
    // ===== UPDATED: SOFT DELETE IMPLEMENTATION =====
    
    /**
     * ✅ UPDATED: Soft delete implementation - solves FK constraint issues
     */
    @Transactional
    public void deleteDocument(Long id) {
        try {
            log.info("🗑️ Starting soft deletion for document ID: {}", id);
            
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
            
            User currentUser = getCurrentUser();
            
            if (!canDeleteDocument(document, currentUser)) {
                throw new SecurityException("You don't have permission to delete this document");
            }
            
            // ✅ Check if already deleted
            if (Boolean.TRUE.equals(document.getDeleted())) {
                throw new IllegalStateException("Document is already deleted");
            }
            
            // ✅ Soft delete - just mark as deleted instead of physical deletion
            document.setDeleted(true);
            document.setDeletedAt(LocalDateTime.now());
            document.setDeletedBy(currentUser.getUsername());
            
            documentRepository.save(document);
            
            // ✅ Log audit
            auditService.logDocumentDeletion(document, currentUser);
            
            log.info("✅ Document {} soft deleted successfully by {}", 
                     document.getOriginalFilename(), currentUser.getUsername());
            
        } catch (EntityNotFoundException | SecurityException | IllegalStateException e) {
            log.error("❌ Delete validation failed for document {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to soft delete document {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete document: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ NEW: Restore soft deleted document
     */
    @Transactional
    public DocumentDTO restoreDocument(Long id) {
        try {
            log.info("🔄 Restoring document ID: {}", id);
            
            Document document = documentRepository.findByIdIncludingDeleted(id)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
            
            User currentUser = getCurrentUser();
            
            if (!canRestoreDocument(document, currentUser)) {
                throw new SecurityException("You don't have permission to restore this document");
            }
            
            if (!Boolean.TRUE.equals(document.getDeleted())) {
                throw new IllegalStateException("Document is not deleted");
            }
            
            // ✅ Restore document
            document.setDeleted(false);
            document.setDeletedAt(null);
            document.setDeletedBy(null);
            
            document = documentRepository.save(document);
            
            // ✅ Log audit
            auditService.logDocumentRestoration(document, currentUser);
            
            log.info("✅ Document {} restored successfully by {}", 
                     document.getOriginalFilename(), currentUser.getUsername());
            
            return convertToDTO(document);
            
        } catch (Exception e) {
            log.error("❌ Failed to restore document {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * ✅ NEW: Get deleted documents (trash)
     */
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDeletedDocuments(Pageable pageable) {
        try {
            Page<Document> deletedDocs = documentRepository.findDeletedDocuments(pageable);
            return deletedDocs.map(this::convertToDTO);
        } catch (Exception e) {
            log.error("❌ Failed to fetch deleted documents: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * ✅ NEW: Permanently delete document (Admin only)
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void permanentlyDeleteDocument(Long id) {
        try {
            log.info("🗑️ Permanently deleting document ID: {}", id);
            
            Document document = documentRepository.findDeletedById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Deleted document not found with id: " + id));
            
            // ✅ Delete physical file
            try {
                fileStorageService.deleteFile(document.getFilePath());
                log.info("📁 Physical file deleted: {}", document.getFilePath());
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete physical file: {}", e.getMessage());
            }
            
            // ✅ Delete from database
            documentRepository.delete(document);
            
            log.info("✅ Document {} permanently deleted", document.getOriginalFilename());
            
        } catch (Exception e) {
            log.error("❌ Failed to permanently delete document {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * ✅ NEW: Check if user can restore document
     */
    public boolean canRestoreDocument(Document document, User user) {
        return canDeleteDocument(document, user); // Same permissions as delete
    }
    
    /**
     * ✅ NEW: Helper method for permission checking (for @PreAuthorize)
     */
    public boolean isDocumentOwner(Long documentId, Long userId) {
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            return document != null && document.getUploadedBy().getId().equals(userId);
        } catch (Exception e) {
            log.error("Error checking document ownership: {}", e.getMessage());
            return false;
        }
    }
    
    // ===== KEEP ALL YOUR EXISTING METHODS =====
    
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
    
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getPendingDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").ascending());
        Page<Document> documents = documentRepository.findPendingDocuments(pageable);
        return documents.map(this::convertToDTO);
    }
    
    // ===== SHARE AND METADATA METHODS (UNCHANGED) =====
    
    public DocumentDTO updateDocumentMetadata(Long id, Map<String, Object> metadata) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canUpdateDocument(document, currentUser)) {
            throw new RuntimeException("You don't have permission to update this document");
        }
        
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
    
    public Map<String, Object> generateShareLink(Long id, Map<String, Object> options) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        if (!canShareDocument(document, currentUser)) {
            throw new RuntimeException("You don't have permission to share this document");
        }
        
        Integer expiryHours = (Integer) options.getOrDefault("expiryHours", 24);
        Boolean allowDownload = (Boolean) options.getOrDefault("allowDownload", true);
        String password = (String) options.get("password");
        
        String shareId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);
        
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
        
        String baseUrl = "https://cloud-docs-tan.vercel.app/";
        String shareUrl = baseUrl + "/shared/" + shareId;
        
        Map<String, Object> response = new HashMap<>();
        response.put("shareUrl", shareUrl);
        response.put("shareId", shareId);
        response.put("expiresAt", expiresAt.toString());
        
        auditService.logDocumentShared(document, currentUser, shareId);
        
        return response;
    }
    
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
    
    public DocumentDTO accessSharedDocument(String shareId, String password) {
        DocumentShareLink shareLink = shareLinksRepository.findByShareIdAndActiveTrue(shareId)
                .orElseThrow(() -> new RuntimeException("Share link not found or expired"));
        
        if (shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link has expired");
        }
        
        if (shareLink.getPassword() != null && !shareLink.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        
        shareLink.setAccessCount(shareLink.getAccessCount() + 1);
        shareLink.setLastAccessedAt(LocalDateTime.now());
        shareLinksRepository.save(shareLink);
        
        return convertToDTO(shareLink.getDocument());
    }
    
    public Resource downloadSharedDocument(String shareId, String password) {
        DocumentShareLink shareLink = shareLinksRepository.findByShareIdAndActiveTrue(shareId)
                .orElseThrow(() -> new RuntimeException("Share link not found or expired"));
        
        if (shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link has expired");
        }
        
        if (!shareLink.getAllowDownload()) {
            throw new RuntimeException("Download not allowed for this share link");
        }
        
        if (shareLink.getPassword() != null && !shareLink.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        
        shareLink.setAccessCount(shareLink.getAccessCount() + 1);
        shareLink.setLastAccessedAt(LocalDateTime.now());
        shareLinksRepository.save(shareLink);
        
        return fileStorageService.loadFileAsResource(shareLink.getDocument().getFilePath());
    }
    
    public DocumentDTO getSharedDocumentInfo(String shareId, String password) {
        return accessSharedDocument(shareId, password);
    }
    
    public Map<String, Object> getDocumentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalDocuments = documentRepository.count();
        stats.put("total", totalDocuments);
        
        Map<String, Long> byStatus = new HashMap<>();
        for (DocumentStatus status : DocumentStatus.values()) {
            long count = documentRepository.countByStatus(status);
            byStatus.put(status.name().toLowerCase(), count);
        }
        stats.put("byStatus", byStatus);
        
        Map<String, Long> byCategory = new HashMap<>();
        List<Object[]> categoryStats = documentRepository.countByCategory();
        for (Object[] row : categoryStats) {
            String category = (String) row[0];
            Long count = (Long) row[1];
            byCategory.put(category, count);
        }
        stats.put("byCategory", byCategory);
        
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentUploads = documentRepository.countByUploadDateAfter(weekAgo);
        stats.put("recentUploads", recentUploads);
        
        long totalDownloads = documentRepository.sumDownloadCounts();
        stats.put("totalDownloads", totalDownloads);
        
        return stats;
    }
    
    // ===== OCR METHODS (UNCHANGED) =====
    
 /**
 * 🚫 SIMPLIFIED: OCR disabled - return helpful error
 */
public DocumentDTO saveDocumentWithOCR(Object request, String username) {
    log.info("🚫 OCR upload attempted but OCR is disabled");
    throw new RuntimeException(
        "OCR processing is temporarily disabled for memory optimization. " +
        "Please use regular document upload at /api/documents/upload instead."
    );
}
    
   public DocumentDTO saveDocument(MultipartFile file, String description, String username) {
    try {
        // Use existing getCurrentUser method instead of userService
        User user = getCurrentUser();
        
        // Store file using existing fileStorageService
        String fileName = fileStorageService.storeFile(file);
        
        // ✅ FIXED: Use correct Document constructor
        Document document = new Document(
            fileName,                    // filename
            file.getOriginalFilename(),  // originalFilename  
            fileName,                    // filePath (using same as filename)
            file.getSize(),              // fileSize
            file.getContentType(),       // mimeType
            user                         // uploadedBy
        );
        
        // ✅ FIXED: Set additional properties using CORRECT setters
        document.setDescription(description);
        document.setStatus(DocumentStatus.PENDING); // ✅ Use PENDING instead of ACTIVE
        
        // Save using existing repository
        document = documentRepository.save(document);
        
        // Log audit using existing service
        auditService.logDocumentUpload(document, user);
        
        // ✅ FIXED: Convert using existing method
        return convertToDTO(document);
        
    } catch (Exception e) {
        log.error("❌ Failed to save document: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to save document: " + e.getMessage(), e);
    }
}

    public Map<String, Object> getOCRStatistics(String username) {
        try {
            long totalDocuments = documentRepository.countByUploadedByUsername(username);
            long documentsWithOCR = documentRepository.countByUploadedByUsernameAndHasOcrTrue(username);
            long documentsWithEmbeddings = documentRepository.countByUploadedByUsernameAndEmbeddingGeneratedTrue(username);
            
            Double averageOCRConfidence = documentRepository.getAverageOCRConfidenceByUser(username);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDocuments", totalDocuments);
            stats.put("documentsWithOCR", documentsWithOCR);
            stats.put("documentsWithEmbeddings", documentsWithEmbeddings);
            stats.put("ocrCoverage", totalDocuments > 0 ? (double) documentsWithOCR / totalDocuments : 0.0);
            stats.put("averageOCRConfidence", averageOCRConfidence != null ? averageOCRConfidence : 0.0);
            
            return stats;
        } catch (Exception e) {
            log.error("❌ Failed to get OCR statistics: {}", e.getMessage());
            return Map.of("error", "Failed to get OCR statistics");
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getAIReadyDocuments() {
        try {
            log.info("🤖 Fetching AI-ready documents");
            
            List<Document> aiReadyDocs = documentRepository.findByEmbeddingGeneratedTrue();
            
            List<DocumentDTO> result = aiReadyDocs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
            log.info("✅ Found {} AI-ready documents", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to fetch AI-ready documents: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByOCRStatus(boolean hasOCR) {
        try {
            log.info("🔍 Filtering documents by OCR status: {}", hasOCR);
            
            List<Document> filteredDocs;
            if (hasOCR) {
                filteredDocs = documentRepository.findByHasOcrTrue();
            } else {
                filteredDocs = documentRepository.findByHasOcrFalseOrHasOcrIsNull();
            }
            
            List<DocumentDTO> result = filteredDocs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
            log.info("✅ Found {} documents with OCR status: {}", result.size(), hasOCR);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to filter documents by OCR status: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDocumentsWithOCR(int page, int size, String sortBy, String sortDir) {
        try {
            log.info("📄 Requesting documents with OCR info - page: {}, size: {}", page, size);
            
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Document> documentsWithOCR = documentRepository.findByHasOcrTrue(pageable);
            
            Page<DocumentDTO> result = documentsWithOCR.map(this::convertToDTO);
            
            log.info("✅ Found {} documents with OCR info", result.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to fetch documents with OCR: {}", e.getMessage(), e);
            
            log.warn("Falling back to regular document query");
            return getAllDocuments(page, size, sortBy, sortDir, null, null, null);
        }
    }
    
    // ===== HELPER METHODS =====
    
    private String generateStoredFilename(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return timestamp + "_" + uuid + extension;
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
    
    public DocumentDTO convertToDTO(Document document) {
        try {
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
            dto.setUploadDate(document.getUploadDate());
            dto.setLastModified(document.getLastModified());
            dto.setDownloadCount(document.getDownloadCount());
            dto.setCategory(document.getCategory());
            dto.setDocumentType(document.getDocumentType());
            dto.setRejectionReason(document.getRejectionReason());
            
            try {
                List<String> tags = document.getTags();
                dto.setTags(tags != null ? new ArrayList<>(tags) : new ArrayList<>());
            } catch (LazyInitializationException e) {
                log.warn("⚠️ Could not load tags for document {}: Using safe accessor", document.getId());
                dto.setTags(document.getTagsSafe());
            }
            
            try {
                if (document.getUploadedBy() != null) {
                    dto.setUploadedByName(document.getUploadedBy().getFullName());
                    dto.setUploadedById(document.getUploadedBy().getId());
                } else {
                    dto.setUploadedByName("Unknown");
                    dto.setUploadedById(null);
                }
            } catch (LazyInitializationException e) {
                log.warn("⚠️ Could not load uploadedBy for document {}: Using safe accessor", document.getId());
                dto.setUploadedByName(document.getUploadedByNameSafe());
                dto.setUploadedById(document.getUploadedByIdSafe());
            }
            
            try {
                if (document.getApprovedBy() != null) {
                    dto.setApprovedByName(document.getApprovedBy().getFullName());
                    dto.setApprovalDate(document.getApprovalDate());
                }
            } catch (LazyInitializationException e) {
                log.warn("⚠️ Could not load approvedBy for document {}: Using safe accessor", document.getId());
                dto.setApprovedByName(document.getApprovedByNameSafe());
                dto.setApprovalDate(document.getApprovalDate());
            }
            
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Error converting document {} to DTO: {}", document.getId(), e.getMessage(), e);
            
            DocumentDTO dto = new DocumentDTO();
            dto.setId(document.getId());
            dto.setFilename(document.getFilename());
            dto.setStatus(document.getStatus());
            dto.setTags(new ArrayList<>());
            dto.setUploadedByName("Unknown");
            return dto;
        }
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    private boolean canChangeDocumentStatus(User user) {
    // ✅ FIXED: Use hasRole() method for Many-to-Many system
    return user.hasRole(ERole.ROLE_ADMIN) || user.hasRole(ERole.ROLE_MANAGER);
}
    
    private boolean canDeleteDocument(Document document, User user) {
    // ✅ FIXED: Use hasRole() method for Many-to-Many system
    return user.hasRole(ERole.ROLE_ADMIN) || 
           document.getUploadedBy().getId().equals(user.getId());
}
    
    private boolean canUpdateDocument(Document document, User user) {
    // ✅ FIXED: Use hasRole() method for Many-to-Many system
    return user.hasRole(ERole.ROLE_ADMIN) || 
           user.hasRole(ERole.ROLE_MANAGER) ||
           document.getUploadedBy().getId().equals(user.getId());
}

private boolean canShareDocument(Document document, User user) {
    // ✅ FIXED: Use hasRole() method for Many-to-Many system
    return user.hasRole(ERole.ROLE_ADMIN) || 
           user.hasRole(ERole.ROLE_MANAGER) ||
           document.getUploadedBy().getId().equals(user.getId());
}

private boolean canViewShareLinks(Document document, User user) {
    return canShareDocument(document, user);
}

private boolean canRevokeShareLink(DocumentShareLink shareLink, User user) {
    // ✅ FIXED: Use hasRole() method for Many-to-Many system
    return user.hasRole(ERole.ROLE_ADMIN) || 
           shareLink.getCreatedBy().getId().equals(user.getId()) ||
           shareLink.getDocument().getUploadedBy().getId().equals(user.getId());
}
}
