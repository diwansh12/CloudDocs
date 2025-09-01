package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.UserPrincipal;
import com.clouddocs.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/workflow-instances")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowService workflowService;
    private final UserRepository userRepository;

    /**
     * ✅ UPDATED: Get user's workflow instances with enhanced display using new service
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getMyWorkflows(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("🔍 Fetching workflow instances - status: {}, page: {}, size: {}", 
                    status, page, size);

            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            User currentUser = getCurrentUser(userPrincipal.getId());
            
            log.info("🔍 Current user: {} with role: {}", currentUser.getUsername(), currentUser.getRole());

            // ✅ UPDATED: Use enhanced service method with proper filtering
            Map<String, Object> response;
            
            if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
                // For admin/manager, we can implement a special service method or use existing one
                log.info("🔑 Admin/Manager access - using enhanced service method");
                response = workflowService.getUserWorkflowsWithDetails(currentUser.getId(), page, size, status);
            } else {
                // Regular user access
                log.info("👤 Regular user access - fetching user's workflow instances");
                response = workflowService.getUserWorkflowsWithDetails(currentUser.getId(), page, size, status);
            }
            
            log.info("✅ Returning workflow instances for user {} (role: {})", 
                    userPrincipal.getUsername(), currentUser.getRole());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting workflow instances: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(Map.of(
                "workflows", new ArrayList<>(),
                "currentPage", page,
                "pageSize", size,
                "totalItems", 0L,
                "totalPages", 0,
                "hasNext", false,
                "hasPrevious", false
            ));
        }
    }

    /**
     * ✅ UPDATED: Search workflow instances using service
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> searchWorkflowInstances(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.debug("Searching workflow instances with query: {}", q);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ UPDATED: Use service method for enhanced search
            Map<String, Object> response = workflowService.getUserWorkflowsWithDetails(
                userPrincipal.getId(), page, size, "All Statuses");
            
            // Filter results based on search query
            @SuppressWarnings("unchecked")
            List<WorkflowInstanceDTO> workflows = (List<WorkflowInstanceDTO>) response.get("workflows");
            
            List<WorkflowInstanceDTO> filteredResults = workflows.stream()
                .filter(w -> matchesSearchQuery(w, q))
                .collect(Collectors.toList());
            
            // Update response with filtered results
            response.put("workflows", filteredResults);
            response.put("totalItems", (long) filteredResults.size());
            response.put("totalPages", 1);
            response.put("hasNext", false);
            response.put("hasPrevious", false);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error searching workflow instances: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("workflows", new ArrayList<>(), "totalItems", 0L));
        }
    }

    /**
     * ✅ UPDATED: Get specific workflow instance by ID using service
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowInstance(@PathVariable Long id) {
        try {
            log.debug("Fetching workflow instance with ID: {}", id);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ UPDATED: Use enhanced service method
            WorkflowInstanceDTO workflow = workflowService.getWorkflowDetailsWithTasks(id, userPrincipal.getId());
            
            return ResponseEntity.ok(workflow);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting workflow instance {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found");
        }
    }

    /**
     * ✅ UPDATED: Get workflow instance with detailed task information using service
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/details")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowWithDetails(@PathVariable Long id) {
        try {
            log.debug("Fetching detailed workflow instance with ID: {}", id);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ UPDATED: Use enhanced service method for detailed workflow
            WorkflowInstanceDTO workflow = workflowService.getWorkflowDetailsWithTasks(id, userPrincipal.getId());
            
            log.debug("✅ Returning detailed workflow instance: {}", id);
            return ResponseEntity.ok(workflow);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting detailed workflow instance {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to load workflow details: " + e.getMessage());
        }
    }

    /**
     * ✅ UPDATED: Cancel workflow instance using service
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<Map<String, Object>> cancelWorkflowInstance(@PathVariable Long id, 
                                                                      @RequestParam(required = false) String reason) {
        try {
            log.info("Cancelling workflow instance: {}", id);
            
            // ✅ UPDATED: Use enhanced service method
            WorkflowInstanceDTO cancelledWorkflow = workflowService.cancelWorkflow(id, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow instance cancelled successfully");
            response.put("workflow", cancelledWorkflow);
            response.put("workflowId", id);
            response.put("reason", reason);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error cancelling workflow instance {}: {}", id, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to cancel workflow instance: " + e.getMessage());
            error.put("workflowId", id);
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ UPDATED: Get workflow instance history - keeping original implementation but with better error handling
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/history")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getWorkflowHistory(@PathVariable Long id) {
        try {
            log.debug("Fetching history for workflow instance: {}", id);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ UPDATED: Get workflow through service first to ensure access control
            WorkflowInstanceDTO workflow = workflowService.getWorkflowDetailsWithTasks(id, userPrincipal.getId());
            
            // Extract history from the workflow DTO
            List<Map<String, Object>> historyDTOs = new ArrayList<>();
            
            if (workflow.getHistory() != null && !workflow.getHistory().isEmpty()) {
                historyDTOs = workflow.getHistory().stream()
                    .map(history -> {
                        Map<String, Object> historyDto = new HashMap<>();
                        historyDto.put("id", history.getId());
                        historyDto.put("action", history.getAction());
                        historyDto.put("details", history.getDetails());
                        historyDto.put("actionDate", history.getActionDate() != null ? 
                            history.getActionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
                        historyDto.put("performedByName", history.getPerformedByName());
                        historyDto.put("performedById", history.getPerformedById());
                        return historyDto;
                    })
                    .collect(Collectors.toList());
            }
            
            return ResponseEntity.ok(historyDTOs);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting workflow history {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * ✅ UPDATED: Add comment to workflow instance - simplified implementation
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/comments")
    @Transactional
    public ResponseEntity<Map<String, Object>> addWorkflowComment(@PathVariable Long id, 
                                                                  @RequestBody Map<String, String> request) {
        try {
            String comment = request.get("comment");
            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Comment cannot be empty"));
            }
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ Note: This functionality might need to be added to WorkflowService
            // For now, return a placeholder response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment functionality needs to be implemented in WorkflowService");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error adding comment to workflow {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add comment"));
        }
    }

    /**
     * ✅ UPDATED: Get workflow instance comments using service
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/comments")
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> getWorkflowComments(@PathVariable Long id) {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ UPDATED: Get workflow through service
            WorkflowInstanceDTO workflow = workflowService.getWorkflowDetailsWithTasks(id, userPrincipal.getId());
            
            List<String> comments = new ArrayList<>();
            if (workflow.getComments() != null && !workflow.getComments().trim().isEmpty()) {
                String[] commentLines = workflow.getComments().split("\n");
                comments.addAll(Arrays.asList(commentLines));
            }
            
            return ResponseEntity.ok(comments);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting comments for workflow {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * ✅ NEW: Get all user tasks with enhanced details
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tasks/mine")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getMyTasks() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ NEW: Use enhanced service method for detailed tasks
            List<Map<String, Object>> tasks = workflowService.getMyTasksWithDetails(userPrincipal.getId());
            
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            log.error("❌ Error getting user tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * ✅ NEW: Process task action through service
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/tasks/{taskId}/action")
    @Transactional
    public ResponseEntity<Map<String, Object>> processTaskAction(
            @PathVariable Long taskId,
            @RequestParam String action,
            @RequestParam(required = false) String comments) {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ NEW: Use enhanced service method for task processing
            Map<String, Object> result = workflowService.processTaskActionWithUser(
                taskId, action, comments, userPrincipal.getId());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Error processing task action: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to process task action: " + e.getMessage());
            error.put("taskId", taskId);
            error.put("action", action);
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ NEW: Get workflow statistics for dashboard
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getWorkflowStatistics() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // ✅ Get user workflows to calculate statistics
            Map<String, Object> workflowsResponse = workflowService.getUserWorkflowsWithDetails(
                userPrincipal.getId(), 0, 1000, "All Statuses");
            
            @SuppressWarnings("unchecked")
            List<WorkflowInstanceDTO> workflows = (List<WorkflowInstanceDTO>) workflowsResponse.get("workflows");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalWorkflows", workflows.size());
            stats.put("inProgress", workflows.stream().filter(w -> w.getStatus() == WorkflowStatus.IN_PROGRESS).count());
            stats.put("approved", workflows.stream().filter(w -> w.getStatus() == WorkflowStatus.APPROVED).count());
            stats.put("rejected", workflows.stream().filter(w -> w.getStatus() == WorkflowStatus.REJECTED).count());
            stats.put("cancelled", workflows.stream().filter(w -> w.getStatus() == WorkflowStatus.CANCELLED).count());
            stats.put("pending", workflows.stream().filter(w -> w.getStatus() == WorkflowStatus.PENDING).count());
            
            // Task statistics
            List<Map<String, Object>> tasks = workflowService.getMyTasksWithDetails(userPrincipal.getId());
            stats.put("pendingTasks", tasks.size());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Error getting workflow statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());
        }
    }

    // ===== HELPER METHODS =====

    /**
     * ✅ UPDATED: Search query matching for WorkflowInstanceDTO
     */
    private boolean matchesSearchQuery(WorkflowInstanceDTO workflow, String query) {
        String searchTerm = query.toLowerCase().trim();
        
        // Search in title
        if (workflow.getTitle() != null && workflow.getTitle().toLowerCase().contains(searchTerm)) {
            return true;
        }
        
        // Search in document name
        if (workflow.getDocumentName() != null && workflow.getDocumentName().toLowerCase().contains(searchTerm)) {
            return true;
        }
        
        // Search in template name
        if (workflow.getTemplateName() != null && workflow.getTemplateName().toLowerCase().contains(searchTerm)) {
            return true;
        }
        
        // Search in initiator name
        if (workflow.getInitiatedByName() != null && workflow.getInitiatedByName().toLowerCase().contains(searchTerm)) {
            return true;
        }
        
        return false;
    }

    /**
     * ✅ Get current user principal
     */
    private UserPrincipal getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    /**
     * ✅ Get current user entity
     */
    private User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
