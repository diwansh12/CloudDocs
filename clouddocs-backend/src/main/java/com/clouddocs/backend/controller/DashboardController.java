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


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class DashboardController {

    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Get dashboard statistics with role-based access
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || auth.getAuthority().equals("ROLE_MANAGER"));
            
            Map<String, Object> stats = new HashMap<>();
            
            if (isAdmin) {
                // Admin can see all statistics
                stats.put("totalDocuments", documentRepository.count());
                stats.put("pendingDocuments", documentRepository.countByStatus(DocumentStatus.PENDING));
                stats.put("approvedDocuments", documentRepository.countByStatus(DocumentStatus.APPROVED));
                stats.put("rejectedDocuments", documentRepository.countByStatus(DocumentStatus.REJECTED));
                stats.put("totalUsers", userRepository.count());
                
                LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
                stats.put("recentUploads", documentRepository.findByUploadDateBetween(
                    weekAgo, LocalDateTime.now(), PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());
            } else {
                // Regular user sees only their own statistics
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                Long userId = userPrincipal.getId();
                
                long userDocs = documentRepository.countByUploadedById(userId);
                long userPending = documentRepository.countByUploadedByIdAndStatus(userId, DocumentStatus.PENDING);
                long userApproved = documentRepository.countByUploadedByIdAndStatus(userId, DocumentStatus.APPROVED);
                
                stats.put("totalDocuments", userDocs);
                stats.put("pendingDocuments", userPending);
                stats.put("approvedDocuments", userApproved);
                stats.put("rejectedDocuments", 0L);
                stats.put("totalUsers", 1L);
                stats.put("recentUploads", userDocs);
            }
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch dashboard statistics: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get recent documents with proper DTO mapping and role-based access
     */
    /**
 * Get recent documents with proper DTO mapping and role-based access
 */
 @GetMapping("/recent-documents")
    @Transactional(readOnly = true)  // ✅ ADD THIS ANNOTATION
    public ResponseEntity<?> getRecentDocuments(@RequestParam(defaultValue = "10") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                    auth.getAuthority().equals("ROLE_MANAGER"));
            
            Page<Document> documents;
            
            if (isAdmin) {
                // ✅ Use JOIN FETCH query
                documents = documentRepository.findRecentDocuments(PageRequest.of(0, limit));
            } else {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                documents = documentRepository.findByUploadedByIdOrderByUploadDateDesc(
                    userPrincipal.getId(), PageRequest.of(0, limit));
            }
            
            // ✅ Convert to DTOs within transaction
            var documentDTOs = documents.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(documentDTOs);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch recent documents: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/recent-documents/temp-fix")
@Transactional(readOnly = true)
public ResponseEntity<List<Map<String, Object>>> getRecentDocumentsTemp(
        @RequestParam(defaultValue = "4") int limit) {
    
    return ResponseEntity.ok(new ArrayList<>());
}


private Map<String, Object> convertToDTO(Document doc) {
    Map<String, Object> dto = new HashMap<>();
    
    dto.put("id", doc.getId());
    dto.put("filename", doc.getFilename());
    dto.put("originalFilename", doc.getOriginalFilename());
    dto.put("description", doc.getDescription());
    dto.put("fileSize", doc.getFileSize());
    dto.put("formattedFileSize", doc.getFormattedFileSize());
    dto.put("mimeType", doc.getMimeType());
    dto.put("status", doc.getStatus());
    dto.put("versionNumber", doc.getVersionNumber());
    dto.put("uploadDate", doc.getUploadDate());
    dto.put("lastModified", doc.getLastModified());
    dto.put("downloadCount", doc.getDownloadCount());
    dto.put("tags", doc.getTags());
    dto.put("category", doc.getCategory());
    dto.put("documentType", doc.getDocumentType());
    
    // ✅ Now safe to access lazy-loaded relationship within transaction
    if (doc.getUploadedBy() != null) {
        dto.put("uploadedByName", doc.getUploadedBy().getFullName());
        dto.put("uploadedById", doc.getUploadedBy().getId());
    } else {
        dto.put("uploadedByName", "Unknown");
        dto.put("uploadedById", null);
    }
    
    if (doc.getApprovedBy() != null) {
        dto.put("approvedByName", doc.getApprovedBy().getFullName());
        dto.put("approvalDate", doc.getApprovalDate());
    }
    
    dto.put("rejectionReason", doc.getRejectionReason());
    
    return dto;
}

}
