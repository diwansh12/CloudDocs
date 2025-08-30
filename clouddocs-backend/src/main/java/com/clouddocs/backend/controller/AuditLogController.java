package com.clouddocs.backend.controller;

import com.clouddocs.backend.entity.AuditLog;
import com.clouddocs.backend.entity.WorkflowInstance;
import com.clouddocs.backend.repository.AuditLogRepository;
import com.clouddocs.backend.repository.WorkflowInstanceRepository;
import com.clouddocs.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;           // ✅ ADDED: Missing import
import org.springframework.http.MediaType;            // ✅ ADDED: Missing import
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    /**
     * Basic GET /api/audit endpoint
     */
    @GetMapping("")
    public ResponseEntity<?> getAllAuditLogs() {
        try {
            log.info("🔍 Fetching all audit logs");
            
            if (auditService == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "AuditService not available");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(500).body(errorResponse);
            }
            
            List<AuditLog> logs = auditService.getAllAuditLogs();
            
            log.info("✅ Retrieved {} audit logs", logs.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", logs.size());
            response.put("data", logs);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error fetching all audit logs: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch audit logs");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for testing
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "audit-api");
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        if (auditService != null) {
            try {
                List<AuditLog> testLogs = auditService.getAllAuditLogs();
                health.put("database", "connected");
                health.put("totalAuditLogs", testLogs.size());
            } catch (Exception e) {
                health.put("database", "error: " + e.getMessage());
                health.put("status", "DEGRADED");
            }
        } else {
            health.put("database", "service not available");
            health.put("status", "DOWN");
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get audit trail for specific workflow
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<?> getWorkflowAuditTrail(@PathVariable String workflowId) {
        try {
            log.info("🔍 Fetching audit trail for workflow: {}", workflowId);
            
            List<AuditLog> auditTrail = auditService.getWorkflowAuditTrail(workflowId);
            
            log.info("✅ Found {} audit entries for workflow: {}", auditTrail.size(), workflowId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("workflowId", workflowId);
            response.put("count", auditTrail.size());
            response.put("data", auditTrail);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching audit trail for workflow {}: {}", workflowId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch workflow audit trail");
            errorResponse.put("workflowId", workflowId);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get all audit logs (admin only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAllAuditLogsAdmin() {
        try {
            List<AuditLog> logs = auditService.getAllAuditLogs();
            log.info("✅ Retrieved {} total audit logs for admin", logs.size());
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("❌ Error fetching all audit logs for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * ✅ FIXED: Export audit logs as CSV with proper imports
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditLogsCSV(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        
        try {
            log.info("🔄 Exporting audit logs to CSV with filters - q: {}, user: {}, type: {}", q, user, type);
            
            // Parse dates
            LocalDate fromDate = null;
            LocalDate toDate = null;
            
            if (from != null && !from.isEmpty()) {
                fromDate = LocalDate.parse(from);
            }
            if (to != null && !to.isEmpty()) {
                toDate = LocalDate.parse(to);
            }
            
            // Fetch filtered audit logs
            List<AuditLog> auditLogs = auditLogRepository.search(q, user, type, fromDate, toDate);
            
            log.info("✅ Found {} audit logs for export", auditLogs.size());
            
            // Generate CSV
            byte[] csvBytes = generateCSV(auditLogs);
            
            // Create filename with timestamp
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
            String filename = "audit_logs_" + timestamp + ".csv";
            
            // ✅ FIXED: HttpHeaders and MediaType now properly imported
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvBytes.length);
            
            log.info("✅ CSV export completed - {} bytes", csvBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
                
        } catch (Exception e) {
            log.error("❌ Error exporting audit logs to CSV: {}", e.getMessage(), e);
            
            // Return error response
            String errorMsg = "Error: " + e.getMessage();
            return ResponseEntity.status(500)
                .contentType(MediaType.TEXT_PLAIN)
                .body(errorMsg.getBytes());
        }
    }
    
    /**
     * Generate CSV content from audit logs
     */
    private byte[] generateCSV(List<AuditLog> auditLogs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        
        // CSV Header
        writer.println("ID,Activity,Linked Item,User,Timestamp,Status");
        
        // CSV Data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (AuditLog log : auditLogs) {
            StringBuilder line = new StringBuilder();
            
            // Escape CSV values (handle commas and quotes)
            line.append(escapeCSV(String.valueOf(log.getId()))).append(",");
            line.append(escapeCSV(log.getActivity())).append(",");
            line.append(escapeCSV(log.getLinkedItem() != null ? log.getLinkedItem() : "")).append(",");
            line.append(escapeCSV(log.getUser())).append(",");
            line.append(escapeCSV(log.getTimestamp().format(formatter))).append(",");
            line.append(escapeCSV(log.getStatus().toString()));
            
            writer.println(line.toString());
        }
        
        writer.flush();
        writer.close();
        
        return outputStream.toByteArray();
    }
    
    /**
     * Escape CSV values to handle commas and quotes
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
    
    /**
     * Search audit logs with filters
     */
    @GetMapping("/search")
    public ResponseEntity<List<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String status) {
        try {
            List<AuditLog> results = auditLogRepository.search(query, user, status, null, null);
            log.info("✅ Search returned {} results", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("❌ Error searching audit logs: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * Backfill audit logs for existing workflows (admin only)
     */
    @PostMapping("/backfill")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> backfillAuditLogs(Authentication authentication) {
        try {
            List<WorkflowInstance> workflows = workflowInstanceRepository.findAll();
            int auditEntriesCreated = 0;
            
            String currentUser = authentication.getName();
            
            for (WorkflowInstance workflow : workflows) {
                try {
                    // Log creation
                    auditService.logWorkflowAction(
                        "Workflow Created: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                        workflow.getId().toString(),
                        workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "unknown"
                    );
                    auditEntriesCreated++;
                    
                    // Handle all workflow statuses
                    switch (workflow.getStatus()) {
                        case APPROVED:
                            auditService.logWorkflowAction(
                                "Workflow Approved: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case REJECTED:
                            auditService.logWorkflowAction(
                                "Workflow Rejected: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case CANCELLED:
                            auditService.logWorkflowAction(
                                "Workflow Cancelled: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case IN_PROGRESS:
                            auditService.logWorkflowAction(
                                "Workflow In Progress: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case PENDING:
                            auditService.logWorkflowAction(
                                "Workflow Pending: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case EXPIRED:
                            auditService.logWorkflowAction(
                                "Workflow Expired: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case ON_HOLD:
                            auditService.logWorkflowAction(
                                "Workflow On Hold: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        case COMPLETED:
                            auditService.logWorkflowAction(
                                "Workflow Completed: " + (workflow.getTemplate() != null ? workflow.getTemplate().getName() : "Unknown Template") + " (Backfilled)",
                                workflow.getId().toString(),
                                workflow.getInitiatedBy() != null ? workflow.getInitiatedBy().getUsername() : "system"
                            );
                            auditEntriesCreated++;
                            break;
                            
                        default:
                            log.warn("Unknown workflow status: {} for workflow {}", workflow.getStatus(), workflow.getId());
                            break;
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to backfill audit for workflow {}: {}", workflow.getId(), e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Backfill completed successfully");
            response.put("workflowsProcessed", workflows.size());
            response.put("auditEntriesCreated", auditEntriesCreated);
            response.put("processedBy", currentUser);
            
            log.info("✅ Backfilled {} audit entries for {} workflows by {}", 
                auditEntriesCreated, workflows.size(), currentUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error during audit backfill: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Backfill failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get audit statistics (admin only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        try {
            List<AuditLog> allLogs = auditService.getAllAuditLogs();
            
            long totalLogs = allLogs.size();
            long successLogs = allLogs.stream().filter(log -> log.getStatus() == AuditLog.Status.SUCCESS).count();
            long failedLogs = allLogs.stream().filter(log -> log.getStatus() == AuditLog.Status.FAILED).count();
            long workflowLogs = allLogs.stream().filter(log -> log.getActivity().toLowerCase().contains("workflow")).count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAuditLogs", totalLogs);
            stats.put("successfulActions", successLogs);
            stats.put("failedActions", failedLogs);
            stats.put("workflowRelatedLogs", workflowLogs);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Error fetching audit stats: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
