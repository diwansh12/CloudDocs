package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.DocumentDTO;
import com.clouddocs.backend.dto.DocumentUploadRequest;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.service.DocumentService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    /**
     * Upload a new document
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags) {
        
        try {
            DocumentUploadRequest request = new DocumentUploadRequest(description, category, tags);
            DocumentDTO documentDTO = documentService.uploadDocument(file, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document uploaded successfully");
            response.put("document", documentDTO);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload document: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all documents with pagination, sorting, and filtering
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String category) {
        
        try {
            Page<DocumentDTO> documents = documentService.getAllDocuments(
                page, size, sortBy, sortDir, search, status, category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("documents", documents.getContent());
            response.put("currentPage", documents.getNumber());
            response.put("totalItems", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("hasNext", documents.hasNext());
            response.put("hasPrevious", documents.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch documents: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get user's own documents
     */
    @GetMapping("/my-documents")
    public ResponseEntity<Map<String, Object>> getMyDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Page<DocumentDTO> documents = documentService.getMyDocuments(page, size, sortBy, sortDir);
            
            Map<String, Object> response = new HashMap<>();
            response.put("documents", documents.getContent());
            response.put("currentPage", documents.getNumber());
            response.put("totalItems", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("hasNext", documents.hasNext());
            response.put("hasPrevious", documents.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch your documents: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id) {
        try {
            DocumentDTO document = documentService.getDocumentById(id);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ✅ NEW: Update document metadata
     */
    @PutMapping("/{id}/metadata")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateDocumentMetadata(
            @PathVariable Long id,
            @RequestBody Map<String, Object> metadata) {
        
        try {
            DocumentDTO updatedDocument = documentService.updateDocumentMetadata(id, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document metadata updated successfully");
            response.put("document", updatedDocument);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update metadata: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * ✅ NEW: Generate share link
     */
    @PostMapping("/{id}/share")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> generateShareLink(
            @PathVariable Long id,
            @RequestBody Map<String, Object> options) {
        
        try {
            Map<String, Object> shareLink = documentService.generateShareLink(id, options);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Share link generated successfully");
            response.put("shareUrl", shareLink.get("shareUrl"));
            response.put("expiresAt", shareLink.get("expiresAt"));
            response.put("shareId", shareLink.get("shareId"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate share link: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * ✅ NEW: Get existing share links for a document
     */
    @GetMapping("/{id}/shares")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getShareLinks(@PathVariable Long id) {
        try {
            List<Map<String, Object>> shareLinks = documentService.getShareLinks(id);
            return ResponseEntity.ok(shareLinks);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch share links: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }


    @GetMapping("/ai-ready")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getAIReadyDocuments() {
    try {
        logger.info("🤖 AI-ready documents requested");
        
        // Try to get actual AI-ready documents
        try {
            List<DocumentDTO> aiReadyDocs = documentService.getAIReadyDocuments();
            
            Map<String, Object> response = new HashMap<>();
            response.put("documents", aiReadyDocs);
            response.put("count", aiReadyDocs.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", "success");
            
            logger.info("✅ Found {} AI-ready documents", aiReadyDocs.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception serviceException) {
            logger.warn("Service method not implemented, using placeholder: {}", serviceException.getMessage());
            
            // Fallback response if service method doesn't exist yet
            Map<String, Object> response = new HashMap<>();
            response.put("documents", new ArrayList<>());
            response.put("count", 0);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "AI-ready document retrieval not yet fully implemented");
            response.put("status", "placeholder");
            
            return ResponseEntity.ok(response);
        }
        
    } catch (Exception e) {
        logger.error("❌ Failed to fetch AI-ready documents: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Failed to fetch AI-ready documents");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(500).body(errorResponse);
    }
}

/**
 * ✅ NEW: Filter documents by OCR status
 */
@GetMapping("/filter-ocr")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getDocumentsByOCRStatus(@RequestParam boolean hasOCR) {
    try {
        logger.info("🔍 Filtering documents by OCR status: {}", hasOCR);
        
        List<DocumentDTO> filteredDocs;
        try {
            filteredDocs = documentService.getDocumentsByOCRStatus(hasOCR);
        } catch (Exception e) {
            logger.warn("OCR filtering not implemented, using placeholder: {}", e.getMessage());
            filteredDocs = new ArrayList<>();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("documents", filteredDocs);
        response.put("count", filteredDocs.size());
        response.put("hasOCR", hasOCR);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        logger.error("❌ Failed to filter documents by OCR status: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Failed to filter documents by OCR status");
        errorResponse.put("message", e.getMessage());
        
        return ResponseEntity.status(500).body(errorResponse);
    }
}

/**
 * ✅ NEW: Get documents with OCR information
 */
@GetMapping("/with-ocr")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getDocumentsWithOCR(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "uploadDate") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir) {
    
    try {
        logger.info("📄 Requesting documents with OCR info - page: {}, size: {}", page, size);
        
        try {
            Page<DocumentDTO> documentsWithOCR = documentService.getDocumentsWithOCR(page, size, sortBy, sortDir);
            
            Map<String, Object> response = new HashMap<>();
            response.put("documents", documentsWithOCR.getContent());
            response.put("currentPage", documentsWithOCR.getNumber());
            response.put("totalItems", documentsWithOCR.getTotalElements());
            response.put("totalPages", documentsWithOCR.getTotalPages());
            response.put("hasNext", documentsWithOCR.hasNext());
            response.put("hasPrevious", documentsWithOCR.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("OCR document retrieval not implemented, using regular documents: {}", e.getMessage());
            
            // Fallback to regular documents
            return getAllDocuments(page, size, sortBy, sortDir, null, null, null);
        }
        
    } catch (Exception e) {
        logger.error("❌ Failed to fetch documents with OCR: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Failed to fetch documents with OCR info");
        errorResponse.put("message", e.getMessage());
        
        return ResponseEntity.status(500).body(errorResponse);
    }
}

    /**
     * ✅ NEW: Revoke share link
     */
    @DeleteMapping("/{id}/shares/{shareId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> revokeShareLink(
            @PathVariable Long id,
            @PathVariable String shareId) {
        
        try {
            documentService.revokeShareLink(id, shareId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Share link revoked successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to revoke share link: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * ✅ NEW: Access shared document (public endpoint)
     */
    @PostMapping("/shared/{shareId}/access")
    public ResponseEntity<?> accessSharedDocument(
            @PathVariable String shareId,
            @RequestBody(required = false) Map<String, String> payload) {
        
        try {
            String password = payload != null ? payload.get("password") : null;
            DocumentDTO document = documentService.accessSharedDocument(shareId, password);
            
            return ResponseEntity.ok(document);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to access shared document: " + e.getMessage());
            return ResponseEntity.status(403).body(error);
        }
    }

    /**
     * ✅ NEW: Download shared document (public endpoint)
     */
    @PostMapping("/shared/{shareId}/download")
    public ResponseEntity<?> downloadSharedDocument(
            @PathVariable String shareId,
            @RequestBody(required = false) Map<String, String> payload) {
        
        try {
            String password = payload != null ? payload.get("password") : null;
            Resource resource = documentService.downloadSharedDocument(shareId, password);
            DocumentDTO document = documentService.getSharedDocumentInfo(shareId, password);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to download shared document: " + e.getMessage());
            return ResponseEntity.status(403).body(error);
        }
    }

    /**
     * Update document status (Admin/Manager only)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> updateDocumentStatus(
            @PathVariable Long id,
            @RequestParam DocumentStatus status,
            @RequestParam(required = false) String rejectionReason) {
        
        try {
            DocumentDTO updatedDocument = documentService.updateDocumentStatus(id, status, rejectionReason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document status updated successfully");
            response.put("document", updatedDocument);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Download document
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        try {
            Resource resource = documentService.downloadDocument(id);
            DocumentDTO document = documentService.getDocumentById(id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete document
     */
   @DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @documentService.isDocumentOwner(#id, authentication.principal.id)")
public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
    try {
        documentService.deleteDocument(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Document deleted successfully");
        response.put("documentId", id);
        response.put("deletedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
        
    } catch (EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Document not found", "documentId", id));
            
    } catch (AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "You don't have permission to delete this document", "documentId", id));
            
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "Document cannot be deleted: " + e.getMessage(), "documentId", id));
            
    } catch (Exception e) {
        logger.error("Failed to delete document {}: {}", id, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to delete document", "documentId", id));
    }
}
    /**
     * Update document (legacy endpoint - redirects to metadata update)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUploadRequest request) {
        
        try {
            DocumentDTO updatedDocument = documentService.updateDocument(id, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document updated successfully");
            response.put("document", updatedDocument);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get pending documents for approval (Manager/Admin only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    @Transactional(readOnly = true)  // ✅ ADD TRANSACTION
    public ResponseEntity<Map<String, Object>> getPendingDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {  // ✅ Use size=3 as expected by frontend
        
        try {
            Page<DocumentDTO> documents = documentService.getPendingDocuments(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("documents", documents.getContent());
            response.put("currentPage", documents.getNumber());
            response.put("totalItems", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("hasNext", documents.hasNext());
            response.put("hasPrevious", documents.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch pending documents: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get all categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        try {
            List<String> categories = documentService.getAllCategories();
            
            if (categories.isEmpty()) {
                categories = Arrays.asList(
                    "Legal", "Marketing", "HR", "Finance", 
                    "Technical", "General", "Reports", "Contracts"
                );
            }
            
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            List<String> defaultCategories = Arrays.asList(
                "Legal", "Marketing", "HR", "Finance", 
                "Technical", "General", "Reports", "Contracts"
            );
            return ResponseEntity.ok(defaultCategories);
        }
    }

    /**
     * Get all tags
     */
    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        try {
            List<String> tags = documentService.getAllTags();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ NEW: Get document statistics (Admin/Manager only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getDocumentStats() {
        try {
            Map<String, Object> stats = documentService.getDocumentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch statistics: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Bulk approve documents (Admin/Manager only)
     */
    @PutMapping("/bulk-approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> bulkApproveDocuments(@RequestBody List<Long> documentIds) {
        try {
            Map<String, Object> results = new HashMap<>();
            int successCount = 0;
            int errorCount = 0;
            
            for (Long id : documentIds) {
                try {
                    documentService.updateDocumentStatus(id, DocumentStatus.APPROVED, null);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                }
            }
            
            results.put("message", String.format("Bulk approval completed: %d successful, %d failed", 
                       successCount, errorCount));
            results.put("successCount", successCount);
            results.put("errorCount", errorCount);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bulk approval failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * ✅ NEW: Bulk status update (Admin/Manager only)
     */
    @PutMapping("/bulk/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> bulkUpdateStatus(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> documentIds = (List<Long>) request.get("documentIds");
            String status = (String) request.get("status");
            String rejectionReason = (String) request.get("rejectionReason");
            
            int successCount = 0;
            int errorCount = 0;
            
            for (Long id : documentIds) {
                try {
                    documentService.updateDocumentStatus(id, DocumentStatus.valueOf(status), rejectionReason);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                }
            }
            
            Map<String, Object> results = new HashMap<>();
            results.put("message", String.format("Bulk update completed: %d successful, %d failed", 
                       successCount, errorCount));
            results.put("successCount", successCount);
            results.put("errorCount", errorCount);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bulk update failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * ✅ NEW: Bulk delete documents (Admin/Manager only)
     */
   @DeleteMapping("/bulk")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
@Transactional
public ResponseEntity<?> bulkDeleteDocuments(@RequestBody Map<String, Object> request) {
    try {
        @SuppressWarnings("unchecked")
        List<Long> documentIds = (List<Long>) request.get("documentIds");
        
        // ✅ Validate input
        if (documentIds == null || documentIds.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No document IDs provided"));
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        
        for (Long id : documentIds) {
            Map<String, Object> result = new HashMap<>();
            result.put("documentId", id);
            
            try {
                documentService.deleteDocument(id);
                result.put("status", "success");
                result.put("message", "Deleted successfully");
                successCount++;
                
            } catch (EntityNotFoundException e) {
                result.put("status", "failed");
                result.put("error", "Document not found");
                errorCount++;
                
            } catch (AccessDeniedException e) {
                result.put("status", "failed");
                result.put("error", "Access denied");
                errorCount++;
                
            } catch (Exception e) {
                result.put("status", "failed");
                result.put("error", e.getMessage());
                errorCount++;
                logger.error("Failed to delete document {}: {}", id, e.getMessage());
            }
            
            results.add(result);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", String.format("Bulk deletion completed: %d successful, %d failed", 
                   successCount, errorCount));
        response.put("totalProcessed", documentIds.size());
        response.put("successCount", successCount);
        response.put("errorCount", errorCount);
        response.put("results", results); // ✅ Detailed per-document results
        response.put("processedAt", System.currentTimeMillis());
        
        // ✅ Return appropriate status based on results
        if (errorCount == 0) {
            return ResponseEntity.ok(response);
        } else if (successCount == 0) {
            return ResponseEntity.badRequest().body(response);
        } else {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }
        
    } catch (ClassCastException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Invalid request format. Expected 'documentIds' array"));
            
    } catch (Exception e) {
        logger.error("Bulk deletion failed: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Bulk deletion failed: " + e.getMessage()));
    }
}

}
