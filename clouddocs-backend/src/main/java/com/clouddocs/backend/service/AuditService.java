package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.AuditLog;
import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    // ===== ✅ NEW DATABASE LOGGING METHODS =====
    
    /**
     * ✅ NEW: Save audit log to database for workflow actions
     */
    @Transactional
    public void logWorkflowAction(String activity, String workflowId, String username) {
        try {
            AuditLog auditLog = AuditLog.of(
                activity,
                workflowId,
                username,
                AuditLog.Status.SUCCESS
            );
            
            auditLogRepository.save(auditLog);
            
            logger.info("✅ Audit saved to DB: {} by {} for workflow {}", activity, username, workflowId);
            
        } catch (Exception e) {
            logger.error("❌ Failed to save audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ✅ NEW: Save audit log with custom status
     */
    @Transactional
    public void logWorkflowActionWithStatus(String activity, String workflowId, String username, AuditLog.Status status) {
        try {
            AuditLog auditLog = AuditLog.of(
                activity,
                workflowId,
                username,
                status
            );
            
            auditLogRepository.save(auditLog);
            
            logger.info("✅ Audit saved to DB: {} by {} for workflow {} with status {}", 
                activity, username, workflowId, status);
            
        } catch (Exception e) {
            logger.error("❌ Failed to save audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ✅ NEW: Get audit trail for specific workflow
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getWorkflowAuditTrail(String workflowId) {
        try {
            return auditLogRepository.search(
                null,           // q (search term)
                null,           // user
                null,           // status
                null,           // from date
                null            // to date
            ).stream()
            .filter(log -> workflowId.equals(log.getLinkedItem()))
            .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Failed to get workflow audit trail: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * ✅ NEW: Get all audit logs (for admin)
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
    
    // ===== EXISTING METHODS ENHANCED WITH DATABASE SAVING =====
    
    public void logDocumentUpload(Document document, User user) {
        String message = String.format("User %s uploaded document: %s", 
            user.getFullName(), document.getOriginalFilename());
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Document Uploaded: " + document.getOriginalFilename(), 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logDocumentStatusChange(Document document, DocumentStatus oldStatus, DocumentStatus newStatus, User user) {
        String message = String.format("User %s changed document %s status from %s to %s", 
            user.getFullName(), document.getOriginalFilename(), oldStatus, newStatus);
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction(String.format("Document Status Changed: %s -> %s", oldStatus, newStatus), 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logDocumentDownload(Document document, User user) {
        String message = String.format("User %s downloaded document: %s", 
            user.getFullName(), document.getOriginalFilename());
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Document Downloaded: " + document.getOriginalFilename(), 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logDocumentDeletion(Document document, User user) {
        String message = String.format("User %s deleted document: %s", 
            user.getFullName(), document.getOriginalFilename());
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Document Deleted: " + document.getOriginalFilename(), 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logDocumentUpdate(Document document, User user) {
        String message = String.format("User %s updated document: %s", 
            user.getFullName(), document.getOriginalFilename());
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Document Updated: " + document.getOriginalFilename(), 
                         document.getId().toString(), user.getUsername());
    }
    
    // ===== ✅ NEW METHODS FOR SHARE FUNCTIONALITY =====
    
    public void logDocumentShared(Document document, User user, String shareId) {
        String message = String.format("User %s shared document: %s with share link ID: %s", 
            user.getFullName(), document.getOriginalFilename(), shareId);
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Document Shared: " + document.getOriginalFilename(), 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logShareLinkRevoked(Document document, User user, String shareId) {
        String message = String.format("User %s revoked share link ID: %s for document: %s", 
            user.getFullName(), shareId, document.getOriginalFilename());
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Share Link Revoked: " + document.getOriginalFilename(), 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logSharedDocumentAccess(Document document, String shareId, String ipAddress) {
        String message = String.format("Anonymous user accessed shared document: %s via share link ID: %s from IP: %s", 
            document.getOriginalFilename(), shareId, ipAddress != null ? ipAddress : "unknown");
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Shared Document Accessed: " + document.getOriginalFilename(), 
                         document.getId().toString(), "anonymous");
    }
    
    public void logSharedDocumentDownload(Document document, String shareId, String ipAddress) {
        String message = String.format("Anonymous user downloaded shared document: %s via share link ID: %s from IP: %s", 
            document.getOriginalFilename(), shareId, ipAddress != null ? ipAddress : "unknown");
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Shared Document Downloaded: " + document.getOriginalFilename(), 
                         document.getId().toString(), "anonymous");
    }
    
    public void logDocumentMetadataUpdate(Document document, User user, String changes) {
        String message = String.format("User %s updated metadata for document: %s. Changes: %s", 
            user.getFullName(), document.getOriginalFilename(), changes);
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("Document Metadata Updated: " + changes, 
                         document.getId().toString(), user.getUsername());
    }
    
    public void logBulkDocumentOperation(String operation, int successCount, int errorCount, User user) {
        String message = String.format("User %s performed bulk %s operation: %d successful, %d failed", 
            user.getFullName(), operation, successCount, errorCount);
        logger.info("AUDIT: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction(String.format("Bulk Operation: %s (%d success, %d failed)", operation, successCount, errorCount), 
                         null, user.getUsername());
    }
    
    public void logSecurityEvent(String event, User user, String details) {
        String message = String.format("SECURITY EVENT - %s by user %s: %s", 
            event, user != null ? user.getFullName() : "Anonymous", details);
        logger.warn("AUDIT_SECURITY: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction("SECURITY: " + event + " - " + details, 
                         null, user != null ? user.getUsername() : "anonymous");
    }
    
    public void logFailedShareLinkAccess(String shareId, String ipAddress, String reason) {
        String message = String.format("Failed access attempt to share link ID: %s from IP: %s. Reason: %s", 
            shareId, ipAddress != null ? ipAddress : "unknown", reason);
        logger.warn("AUDIT_SECURITY: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowActionWithStatus("Failed Share Link Access: " + reason, 
                                   shareId, "anonymous", AuditLog.Status.FAILED);
    }
    
    public void logSystemCleanup(String operation, int itemsProcessed, String details) {
        String message = String.format("System cleanup: %s processed %d items. Details: %s", 
            operation, itemsProcessed, details);
        logger.info("AUDIT_SYSTEM: {}", message);
        
        // ✅ ADD: Save to database
        logWorkflowAction(String.format("System Cleanup: %s (%d items)", operation, itemsProcessed), 
                         null, "system");
    }
}
