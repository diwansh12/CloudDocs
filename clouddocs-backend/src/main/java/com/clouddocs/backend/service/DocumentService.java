package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.DocumentDTO;
import com.clouddocs.backend.dto.DocumentUploadRequest;
import com.clouddocs.backend.dto.DocumentWithOCRDTO;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.entity.DocumentShareLink;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.repository.DocumentShareLinkRepository;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.exception.UserNotFoundException;

import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
@CacheConfig(cacheNames = "documents")
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

    @Autowired
    private AIEmbeddingService embeddingService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // ===== CACHED DOCUMENT CRUD OPERATIONS =====
    
    /**
     * ✅ CACHED: Upload document with cache warming
     */
    @Caching(evict = {
        @CacheEvict(value = "dashboard-stats", allEntries = true),
        @CacheEvict(value = "documents", key = "'user:' + #result.uploadedById + ':*'", 
                   condition = "#result != null")
    })
    public DocumentDTO uploadDocument(MultipartFile file, DocumentUploadRequest request) {
        try {
            log.info("📤 Uploading document: {}", file.getOriginalFilename());
            
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
            
            DocumentDTO dto = convertToDTO(document);
            
            // ✅ Warm cache with new document
            cacheDocument(dto);
            
            log.info("✅ Document uploaded and cached: {}", dto.getId());
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Failed to upload document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ HEAVILY CACHED: Get all documents with smart caching
     */
    @Cacheable(
        value = "documents", 
        key = "'all:page:' + #page + ':size:' + #size + ':sort:' + #sortBy + ':dir:' + #sortDir + ':search:' + (#search != null ? #search : 'none') + ':status:' + (#status != null ? #status.toString() : 'none') + ':category:' + (#category != null ? #category : 'none')",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getAllDocuments(int page, int size, String sortBy, String sortDir, 
                                           String search, DocumentStatus status, String category) {
        try {
            log.info("🔍 Getting all documents - page: {}, size: {}, search: {}", page, size, search);
            
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
            
            Page<DocumentDTO> result = documents.map(this::convertToDTO);
            log.info("✅ Retrieved {} documents (cached)", result.getTotalElements());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error in getAllDocuments: {}", e.getMessage(), e);
            return Page.empty(PageRequest.of(page, size));
        }
    }

    /**
     * ✅ CACHED: Get user's documents with smart key generation
     */
    @Cacheable(
        value = "documents",
        key = "'user:' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + ':page:' + #page + ':size:' + #size + ':sort:' + #sortBy + ':dir:' + #sortDir",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getMyDocuments(int page, int size, String sortBy, String sortDir) {
        try {
            User currentUser = getCurrentUser();
            log.info("🔍 Getting documents for user: {} - page: {}, size: {}", currentUser.getUsername(), page, size);
            
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : 
                       Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Document> documents = documentRepository.findByUploadedByIdWithTagsAndUsers(currentUser.getId(), pageable);
            Page<DocumentDTO> result = documents.map(this::convertToDTO);
            
            log.info("✅ Retrieved {} user documents (cached)", result.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error getting user documents: {}", e.getMessage(), e);
            User currentUser = getCurrentUser();
            return Page.empty(PageRequest.of(page, size));
        }
    }
    
    /**
     * ✅ HEAVILY CACHED: Get document by ID with long TTL
     */
    @Cacheable(
        value = "documents", 
        key = "'doc:' + #id", 
        unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public DocumentDTO getDocumentById(Long id) {
        try {
            log.info("🔍 Getting document by ID: {} (checking cache first)", id);
            
            Document document = documentRepository.findByIdWithTags(id)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
            
            DocumentDTO dto = convertToDTO(document);
            log.info("✅ Document retrieved: {} (will be cached)", dto.getOriginalFilename());
            
            return dto;
            
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ Document not found: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting document {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve document", e);
        }
    }
    
    /**
     * ✅ CACHE EVICTION: Update document status with comprehensive cache clearing
     */
    @Caching(evict = {
        @CacheEvict(value = "documents", key = "'doc:' + #id"),
        @CacheEvict(value = "documents", allEntries = true, condition = "#result != null"),
        @CacheEvict(value = "dashboard-stats", allEntries = true),
        @CacheEvict(value = "pending-documents", allEntries = true)
    })
    public DocumentDTO updateDocumentStatus(Long id, DocumentStatus status, String rejectionReason) {
        try {
            log.info("📝 Updating document {} status to: {}", id, status);
            
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
            
            User currentUser = getCurrentUser();
            
            if (!canChangeDocumentStatus(currentUser)) {
                throw new SecurityException("You don't have permission to change document status");
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
            
            DocumentDTO dto = convertToDTO(document);
            
            // ✅ Update cache with new status
            cacheDocument(dto);
            
            log.info("✅ Document status updated and cache refreshed: {} -> {}", oldStatus, status);
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Failed to update document status: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * ✅ CACHED: Download with usage tracking
     */
    @Cacheable(value = "document-files", key = "'file:' + #id", unless = "#result == null")
    public Resource downloadDocument(Long id) {
        try {
            log.info("📥 Download requested for document: {}", id);
            
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
            
            // ✅ Update download count asynchronously to avoid cache issues
            updateDownloadCountAsync(id);
            
            User currentUser = getCurrentUser();
            auditService.logDocumentDownload(document, currentUser);
            
            Resource resource = fileStorageService.loadFileAsResource(document.getFilePath());
            log.info("✅ Document download prepared: {}", document.getOriginalFilename());
            
            return resource;
            
        } catch (Exception e) {
            log.error("❌ Download failed for document {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * ✅ COMPREHENSIVE CACHE EVICTION: Soft delete with cache management
     */
    @Caching(evict = {
        @CacheEvict(value = "documents", key = "'doc:' + #id"),
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true),
        @CacheEvict(value = "document-files", key = "'file:' + #id")
    })
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
            
            if (Boolean.TRUE.equals(document.getDeleted())) {
                throw new IllegalStateException("Document is already deleted");
            }
            
            // ✅ Soft delete
            document.setDeleted(true);
            document.setDeletedAt(LocalDateTime.now());
            document.setDeletedBy(currentUser.getUsername());
            
            documentRepository.save(document);
            auditService.logDocumentDeletion(document, currentUser);
            
            log.info("✅ Document {} soft deleted and cache cleared", document.getOriginalFilename());
            
        } catch (Exception e) {
            log.error("❌ Failed to soft delete document {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * ✅ CACHE WARMING: Restore document with cache update
     */
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
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
            auditService.logDocumentRestoration(document, currentUser);
            
            DocumentDTO dto = convertToDTO(document);
            
            // ✅ Warm cache with restored document
            cacheDocument(dto);
            
            log.info("✅ Document {} restored and cached", document.getOriginalFilename());
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Failed to restore document {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * ✅ CACHED: Get deleted documents
     */
    @Cacheable(
        value = "deleted-documents",
        key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDeletedDocuments(Pageable pageable) {
        try {
            log.info("🗑️ Getting deleted documents - page: {}", pageable.getPageNumber());
            
            Page<Document> deletedDocs = documentRepository.findDeletedDocuments(pageable);
            Page<DocumentDTO> result = deletedDocs.map(this::convertToDTO);
            
            log.info("✅ Retrieved {} deleted documents (cached)", result.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to fetch deleted documents: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * ✅ COMPREHENSIVE CACHE EVICTION: Permanent deletion
     */
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "deleted-documents", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true),
        @CacheEvict(value = "document-files", key = "'file:' + #id")
    })
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
            
            log.info("✅ Document {} permanently deleted and all caches cleared", document.getOriginalFilename());
            
        } catch (Exception e) {
            log.error("❌ Failed to permanently delete document {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    // ===== CACHED OCR AND AI METHODS =====
    
    /**
     * ✅ CACHE WARMING: Save document with OCR and cache immediately
     */
    @Caching(evict = {
        @CacheEvict(value = "dashboard-stats", allEntries = true),
        @CacheEvict(value = "ocr-stats", allEntries = true)
    })
    @Transactional
    public DocumentDTO saveDocumentWithOCR(DocumentWithOCRDTO documentWithOCR, String username) {
        try {
            log.info("💾 Saving document with OCR for user: {}", username);
            
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
            
            MultipartFile file = documentWithOCR.getFile();
            OCRResultDTO ocrResult = documentWithOCR.getOcrResult();
            
            String storedFilePath = fileStorageService.storeFile(file);
            
            Document document = new Document();
            document.setOriginalFilename(file.getOriginalFilename());
            document.setFilename(generateStoredFilename(file.getOriginalFilename()));
            document.setFilePath(storedFilePath);
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setDocumentType(determineDocumentType(file.getContentType()));
            document.setUploadedBy(user);
            document.setUploadDate(LocalDateTime.now());
            document.setDescription(documentWithOCR.getDescription());
            document.setCategory(documentWithOCR.getCategory());
            
            // ✅ OCR data
            document.setOcrText(ocrResult.getExtractedText());
            document.setOcrConfidence(ocrResult.getConfidence());
            document.setHasOcr(ocrResult.isSuccess());
            document.setOcrProcessingTime(ocrResult.getProcessingTimeMs());
            
            // ✅ AI embedding
            List<Double> embedding = documentWithOCR.getEmbedding();
            if (embedding != null && !embedding.isEmpty()) {
                document.setEmbedding(embeddingService.embeddingToJson(embedding));
                document.setEmbeddingGenerated(true);
            }
            
            Document savedDocument = documentRepository.save(document);
            DocumentDTO dto = convertToDTO(savedDocument);
            
            // ✅ Cache OCR result for reuse
            if (ocrResult.isSuccess()) {
                cacheOCRResult(generateFileHash(file), ocrResult.getExtractedText());
            }
            
            // ✅ Warm cache with new document
            cacheDocument(dto);
            
            log.info("✅ Saved document with OCR and cached: {} (OCR: {}, Embedding: {})", 
                savedDocument.getOriginalFilename(), 
                document.getHasOcr(),
                document.getEmbeddingGenerated());
            
            return dto;
            
        } catch (Exception e) {
            log.error("❌ Failed to save document with OCR: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save document with OCR processing", e);
        }
    }

    /**
     * ✅ CACHED: OCR statistics with user-specific caching
     */
    @Cacheable(
        value = "ocr-stats",
        key = "'user:' + #username",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Map<String, Object> getOCRStatistics(String username) {
        try {
            log.info("📊 Getting OCR statistics for user: {} (checking cache)", username);
            
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
            stats.put("aiReadyDocuments", documentsWithEmbeddings);
            stats.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ OCR statistics calculated and cached for user: {}", username);
            return stats;
            
        } catch (Exception e) {
            log.error("❌ Failed to get OCR statistics: {}", e.getMessage(), e);
            return Map.of("error", "Failed to get OCR statistics", "timestamp", System.currentTimeMillis());
        }
    }

    /**
     * ✅ CACHED: AI-ready documents
     */
    @Cacheable(
        value = "ai-ready-documents",
        key = "'all'",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<DocumentDTO> getAIReadyDocuments() {
        try {
            log.info("🤖 Fetching AI-ready documents (checking cache)");
            
            List<Document> aiReadyDocs = documentRepository.findByEmbeddingGeneratedTrue();
            List<DocumentDTO> result = aiReadyDocs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
            log.info("✅ Found {} AI-ready documents (cached)", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to fetch AI-ready documents: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * ✅ CACHED: Documents by OCR status
     */
    @Cacheable(
        value = "ocr-filtered-documents",
        key = "'hasOCR:' + #hasOCR",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByOCRStatus(boolean hasOCR) {
        try {
            log.info("🔍 Filtering documents by OCR status: {} (checking cache)", hasOCR);
            
            List<Document> filteredDocs;
            if (hasOCR) {
                filteredDocs = documentRepository.findByHasOcrTrue();
            } else {
                filteredDocs = documentRepository.findByHasOcrFalseOrHasOcrIsNull();
            }
            
            List<DocumentDTO> result = filteredDocs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
            log.info("✅ Found {} documents with OCR status: {} (cached)", result.size(), hasOCR);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to filter documents by OCR status: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * ✅ CACHED: Documents with OCR (paginated)
     */
    @Cacheable(
        value = "documents-with-ocr",
        key = "'page:' + #page + ':size:' + #size + ':sort:' + #sortBy + ':dir:' + #sortDir",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDocumentsWithOCR(int page, int size, String sortBy, String sortDir) {
        try {
            log.info("📄 Requesting documents with OCR info - page: {}, size: {} (checking cache)", page, size);
            
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Document> documentsWithOCR = documentRepository.findByHasOcrTrue(pageable);
            Page<DocumentDTO> result = documentsWithOCR.map(this::convertToDTO);
            
            log.info("✅ Found {} documents with OCR info (cached)", result.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to fetch documents with OCR: {}", e.getMessage(), e);
            return getAllDocuments(page, size, sortBy, sortDir, null, null, null);
        }
    }
    
    // ===== CACHED UTILITY AND STATISTICS METHODS =====
    
    /**
     * ✅ CACHED: Document statistics with dashboard optimization
     */
    @Cacheable(
        value = "dashboard-stats",
        key = "'document-stats'",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Map<String, Object> getDocumentStatistics() {
        try {
            log.info("📈 Getting document statistics (checking cache)");
            
            Map<String, Object> stats = new HashMap<>();
            
            long totalDocuments = documentRepository.count();
            stats.put("total", totalDocuments);
            
            // Status breakdown
            Map<String, Long> byStatus = new HashMap<>();
            for (DocumentStatus status : DocumentStatus.values()) {
                long count = documentRepository.countByStatus(status);
                byStatus.put(status.name().toLowerCase(), count);
            }
            stats.put("byStatus", byStatus);
            
            // Category breakdown
            Map<String, Long> byCategory = new HashMap<>();
            List<Object[]> categoryStats = documentRepository.countByCategory();
            for (Object[] row : categoryStats) {
                String category = (String) row[0];
                Long count = (Long) row[1];
                byCategory.put(category != null ? category : "uncategorized", count);
            }
            stats.put("byCategory", byCategory);
            
            // Recent activity
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentUploads = documentRepository.countByUploadDateAfter(weekAgo);
            stats.put("recentUploads", recentUploads);
            
            long totalDownloads = documentRepository.sumDownloadCounts();
            stats.put("totalDownloads", totalDownloads);
            
            stats.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ Document statistics calculated and cached");
            return stats;
            
        } catch (Exception e) {
            log.error("❌ Failed to get document statistics: {}", e.getMessage(), e);
            return Map.of("error", "Failed to get statistics", "timestamp", System.currentTimeMillis());
        }
    }

    /**
     * ✅ CACHED: Pending documents with short TTL
     */
    @Cacheable(
        value = "pending-documents",
        key = "'page:' + #page + ':size:' + #size",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getPendingDocuments(int page, int size) {
        try {
            log.info("⏳ Getting pending documents - page: {}, size: {} (checking cache)", page, size);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").ascending());
            Page<Document> documents = documentRepository.findPendingDocuments(pageable);
            Page<DocumentDTO> result = documents.map(this::convertToDTO);
            
            log.info("✅ Found {} pending documents (cached)", result.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to get pending documents: {}", e.getMessage(), e);
            return Page.empty(PageRequest.of(page, size));
        }
    }

    /**
     * ✅ CACHED: Categories and tags
     */
    @Cacheable(value = "document-categories", key = "'all'", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        try {
            List<String> categories = documentRepository.findAllCategories();
            log.info("✅ Retrieved {} categories (cached)", categories.size());
            return categories;
        } catch (Exception e) {
            log.error("❌ Failed to get categories: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Cacheable(value = "document-tags", key = "'all'", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        try {
            List<String> tags = documentRepository.findAllTags();
            log.info("✅ Retrieved {} tags (cached)", tags.size());
            return tags;
        } catch (Exception e) {
            log.error("❌ Failed to get tags: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ===== CACHE MANAGEMENT METHODS =====
    
    /**
     * ✅ Cache a document manually
     */
    private void cacheDocument(DocumentDTO document) {
        try {
            redisTemplate.opsForValue().set(
                "clouddocs:cache:documents::doc:" + document.getId(), 
                document, 
                2, 
                TimeUnit.HOURS
            );
            log.debug("💾 Document {} manually cached", document.getId());
        } catch (Exception e) {
            log.warn("⚠️ Failed to manually cache document {}: {}", document.getId(), e.getMessage());
        }
    }
    
    /**
     * ✅ Cache OCR result for reuse
     */
    private void cacheOCRResult(String fileHash, String ocrText) {
        try {
            redisTemplate.opsForValue().set(
                "clouddocs:cache:ocr-results::" + fileHash, 
                ocrText, 
                24, 
                TimeUnit.HOURS
            );
            log.debug("🤖 OCR result cached for hash: {}", fileHash);
        } catch (Exception e) {
            log.warn("⚠️ Failed to cache OCR result: {}", e.getMessage());
        }
    }
    
    /**
     * ✅ Get cached OCR result
     */
    public String getCachedOCRResult(String fileHash) {
        try {
            Object cached = redisTemplate.opsForValue().get("clouddocs:cache:ocr-results::" + fileHash);
            if (cached != null) {
                log.debug("✅ OCR cache hit for hash: {}", fileHash);
                return cached.toString();
            }
            log.debug("❌ OCR cache miss for hash: {}", fileHash);
            return null;
        } catch (Exception e) {
            log.warn("⚠️ Failed to get cached OCR result: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * ✅ Evict user-specific caches
     */
    public void evictUserCaches(String username) {
        try {
            // Evict user documents cache
            Set<String> keys = redisTemplate.keys("clouddocs:cache:documents::user:" + username + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Evicted {} user cache entries for: {}", keys.size(), username);
            }
            
            // Evict OCR stats
            redisTemplate.delete("clouddocs:cache:ocr-stats::user:" + username);
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to evict user caches: {}", e.getMessage());
        }
    }
    
    /**
     * ✅ Update download count asynchronously to avoid cache conflicts
     */
    private void updateDownloadCountAsync(Long documentId) {
        // This should be implemented with @Async to avoid blocking
        try {
            documentRepository.incrementDownloadCount(documentId);
            // Don't evict main document cache as download count is not critical for display
        } catch (Exception e) {
            log.warn("⚠️ Failed to update download count for document {}: {}", documentId, e.getMessage());
        }
    }
    
    // ===== ALL YOUR EXISTING SHARE LINK AND UPDATE METHODS (UNCHANGED) =====
    
    @Caching(evict = {
        @CacheEvict(value = "documents", key = "'doc:' + #id"),
        @CacheEvict(value = "documents", allEntries = true, condition = "#result != null")
    })
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
        
        DocumentDTO dto = convertToDTO(document);
        cacheDocument(dto); // Warm cache
        
        return dto;
    }
    
    @Caching(evict = {
        @CacheEvict(value = "documents", key = "'doc:' + #id"),
        @CacheEvict(value = "documents", allEntries = true, condition = "#result != null")
    })
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
        
        DocumentDTO dto = convertToDTO(document);
        cacheDocument(dto); // Warm cache
        
        return dto;
    }
    
    // ===== ALL SHARE LINK METHODS REMAIN THE SAME =====
    
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
    
    private String generateFileHash(MultipartFile file) {
        try {
            return Integer.toString((file.getOriginalFilename() + file.getSize()).hashCode());
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
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
            
            // ✅ Safe OCR field handling
            dto.setHasOcr(document.getHasOcr() != null ? document.getHasOcr() : false);
            dto.setOcrText(document.getOcrText());
            dto.setOcrConfidence(document.getOcrConfidence() != null ? document.getOcrConfidence() : 0.0);
            dto.setEmbeddingGenerated(document.getEmbeddingGenerated() != null ? document.getEmbeddingGenerated() : false);
            
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
            
            // Fallback DTO
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
    
    // ===== PERMISSION CHECKING METHODS (UNCHANGED) =====
    
    public boolean isDocumentOwner(Long documentId, Long userId) {
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            return document != null && document.getUploadedBy().getId().equals(userId);
        } catch (Exception e) {
            log.error("Error checking document ownership: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean canRestoreDocument(Document document, User user) {
        return canDeleteDocument(document, user);
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
