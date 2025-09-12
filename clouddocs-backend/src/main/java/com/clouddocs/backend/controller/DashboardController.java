package com.clouddocs.backend.controller;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/stats")
public ResponseEntity<Map<String, Object>> getDashboardStats() {
    try {
        logger.info("🔍 Getting dashboard stats");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                auth.getAuthority().equals("ROLE_MANAGER"));
        
        Map<String, Object> stats = new HashMap<>();
        
        if (isAdmin) {
            // ✅ FIXED: Use methods that exclude deleted documents
            stats.put("totalDocuments", documentRepository.countByDeletedFalse());
            stats.put("pendingDocuments", documentRepository.countByStatusAndDeletedFalse(DocumentStatus.PENDING));
            stats.put("approvedDocuments", documentRepository.countByStatusAndDeletedFalse(DocumentStatus.APPROVED));
            stats.put("rejectedDocuments", documentRepository.countByStatusAndDeletedFalse(DocumentStatus.REJECTED));
            stats.put("totalUsers", userRepository.count());
            
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            stats.put("recentUploads", documentRepository.countByUploadDateAfterAndDeletedFalse(weekAgo));
        } else {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getId();
            
            // ✅ FIXED: Use methods that exclude deleted documents for users
            long userDocs = documentRepository.countByUploadedByIdAndDeletedFalse(userId);
            long userPending = documentRepository.countByUploadedByIdAndStatusAndDeletedFalse(userId, DocumentStatus.PENDING);
            long userApproved = documentRepository.countByUploadedByIdAndStatusAndDeletedFalse(userId, DocumentStatus.APPROVED);
            
            stats.put("totalDocuments", userDocs);
            stats.put("pendingDocuments", userPending);
            stats.put("approvedDocuments", userApproved);
            stats.put("rejectedDocuments", 0L);
            stats.put("totalUsers", 1L);
            stats.put("recentUploads", userDocs);
        }
        
        logger.info("✅ Dashboard stats retrieved successfully");
        return ResponseEntity.ok(stats);
        
    } catch (Exception e) {
        logger.error("❌ Error getting dashboard stats: {}", e.getMessage(), e);
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Failed to fetch dashboard statistics: " + e.getMessage());
        return ResponseEntity.status(500).body(error);
    }
}

    /**
     * ✅ COMPLETELY REWRITTEN: Safe recent documents endpoint
     */
  @GetMapping("/recent-documents")
@Transactional(readOnly = true)
public ResponseEntity<List<Map<String, Object>>> getRecentDocuments(
        @RequestParam(defaultValue = "4") int limit) {
    try {
        logger.info("🔍 Getting recent documents with limit: {}", limit);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.warn("⚠️ No authentication found");
            return ResponseEntity.status(401).body(new ArrayList<>());
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                auth.getAuthority().equals("ROLE_MANAGER"));
        
        Page<Document> documents;
        
        if (isAdmin) {
            logger.debug("🔍 Loading recent documents for admin/manager");
            // ✅ FIXED: Use method that excludes deleted documents
            documents = documentRepository.findRecentDocumentsExcludingDeleted(PageRequest.of(0, limit));
        } else {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            logger.debug("🔍 Loading recent documents for user: {}", userPrincipal.getId());
            // ✅ FIXED: Use method that excludes deleted documents
            documents = documentRepository.findByUploadedByIdOrderByUploadDateDescExcludingDeleted(
                userPrincipal.getId(), PageRequest.of(0, limit));
        }
        
        // ✅ SAFE DTO conversion within transaction
        List<Map<String, Object>> documentDTOs = new ArrayList<>();
        
        for (Document doc : documents.getContent()) {
            try {
                Map<String, Object> dto = createSafeDocumentDTO(doc);
                documentDTOs.add(dto);
            } catch (Exception e) {
                logger.warn("⚠️ Error converting document {}: {}", doc.getId(), e.getMessage());
                // Add minimal DTO on error
                Map<String, Object> minimalDto = new HashMap<>();
                minimalDto.put("id", doc.getId());
                minimalDto.put("filename", doc.getFilename());
                minimalDto.put("status", doc.getStatus().toString());
                minimalDto.put("uploadDate", doc.getUploadDate().toString());
                minimalDto.put("uploadedByName", "Unknown");
                minimalDto.put("tags", new ArrayList<>());
                documentDTOs.add(minimalDto);
            }
        }
        
        logger.info("✅ Successfully loaded {} recent documents", documentDTOs.size());
        return ResponseEntity.ok(documentDTOs);
        
    } catch (Exception e) {
        logger.error("❌ Error getting recent documents: {}", e.getMessage(), e);
        
        // Return empty list instead of error to prevent frontend crashes
        List<Map<String, Object>> emptyList = new ArrayList<>();
        return ResponseEntity.ok(emptyList);
    }
}

    /**
     * ✅ SAFE DTO creation method - handles all lazy loading exceptions
     */
    private Map<String, Object> createSafeDocumentDTO(Document doc) {
        Map<String, Object> dto = new HashMap<>();
        
        try {
            // Basic fields - no lazy loading issues
            dto.put("id", doc.getId());
            dto.put("filename", doc.getFilename());
            dto.put("originalFilename", doc.getOriginalFilename());
            dto.put("description", doc.getDescription());
            dto.put("fileSize", doc.getFileSize());
            dto.put("formattedFileSize", doc.getFormattedFileSize());
            dto.put("mimeType", doc.getMimeType());
            dto.put("status", doc.getStatus().toString());
            dto.put("versionNumber", doc.getVersionNumber());
            dto.put("uploadDate", doc.getUploadDate().toString());
            dto.put("lastModified", doc.getLastModified() != null ? doc.getLastModified().toString() : null);
            dto.put("downloadCount", doc.getDownloadCount());
            dto.put("category", doc.getCategory());
            dto.put("documentType", doc.getDocumentType());
            dto.put("rejectionReason", doc.getRejectionReason());
            
            // ✅ Safe handling of tags collection
            try {
                List<String> tags = doc.getTags();
                dto.put("tags", tags != null ? new ArrayList<>(tags) : new ArrayList<>());
            } catch (Exception e) {
                logger.debug("Could not load tags for document {}, using empty list", doc.getId());
                dto.put("tags", new ArrayList<>());
            }
            
            // ✅ Safe handling of uploadedBy relationship
            try {
                if (doc.getUploadedBy() != null) {
                    dto.put("uploadedByName", doc.getUploadedBy().getFullName());
                    dto.put("uploadedById", doc.getUploadedBy().getId());
                } else {
                    dto.put("uploadedByName", "Unknown");
                    dto.put("uploadedById", null);
                }
            } catch (Exception e) {
                logger.debug("Could not load uploadedBy for document {}, using default", doc.getId());
                dto.put("uploadedByName", "Unknown");
                dto.put("uploadedById", null);
            }
            
            // ✅ Safe handling of approvedBy relationship
            try {
                if (doc.getApprovedBy() != null) {
                    dto.put("approvedByName", doc.getApprovedBy().getFullName());
                    dto.put("approvalDate", doc.getApprovalDate() != null ? doc.getApprovalDate().toString() : null);
                } else {
                    dto.put("approvedByName", null);
                    dto.put("approvalDate", null);
                }
            } catch (Exception e) {
                logger.debug("Could not load approvedBy for document {}", doc.getId());
                dto.put("approvedByName", null);
                dto.put("approvalDate", null);
            }
            
            return dto;
            
        } catch (Exception e) {
            logger.error("❌ Error creating DTO for document {}: {}", doc.getId(), e.getMessage());
            
            // Return minimal DTO on error
            Map<String, Object> minimalDto = new HashMap<>();
            minimalDto.put("id", doc.getId());
            minimalDto.put("filename", doc.getFilename());
            minimalDto.put("status", doc.getStatus().toString());
            minimalDto.put("uploadDate", doc.getUploadDate().toString());
            minimalDto.put("uploadedByName", "Unknown");
            minimalDto.put("tags", new ArrayList<>());
            return minimalDto;
        }
    }
}

