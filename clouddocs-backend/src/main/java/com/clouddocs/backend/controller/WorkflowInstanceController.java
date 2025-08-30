package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowInstance;
import com.clouddocs.backend.entity.WorkflowStatus;
import com.clouddocs.backend.mapper.WorkflowMapper;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.WorkflowInstanceRepository;
import com.clouddocs.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowService workflowService;
    private final UserRepository userRepository;

    /**
     * Get a specific workflow instance with details
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowInstanceDTO> getWorkflow(@PathVariable Long id) {
        log.debug("Fetching workflow instance with ID: {}", id);
        
        WorkflowInstance instance = instanceRepository.findByIdWithBasicDetails(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
        
        // Add authorization check
        User currentUser = getCurrentUser();
        if (!canAccessWorkflow(currentUser, instance)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this workflow");
        }
        
        WorkflowInstanceDTO dto = WorkflowMapper.toInstanceDTO(instance);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get workflow instance with complete task details
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/details")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowWithTasks(@PathVariable Long id) {
        log.debug("Fetching workflow instance with tasks for ID: {}", id);
        
        WorkflowInstance instance = instanceRepository.findByIdWithTasks(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
        
        User currentUser = getCurrentUser();
        if (!canAccessWorkflow(currentUser, instance)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this workflow");
        }
        
        WorkflowInstanceDTO dto = WorkflowMapper.toInstanceDTO(instance);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get user's workflows with filtering and pagination - FIXED: Proper Page handling
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public ResponseEntity<Map<String, Object>> getMyWorkflows(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching workflows for current user with filters - status: {}, templateId: {}, from: {}, to: {}", 
                 status, templateId, from, to);

        User currentUser = getCurrentUser();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowInstance> pageResult;

        // FIXED: Proper Page handling for all filter combinations
        if (status != null && templateId != null) {
            // Both status and template filters
            pageResult = instanceRepository.findByInitiatedByAndStatusAndTemplateId(currentUser, status, templateId, pageable);
        } else if (status != null) {
            // Status filter only
            pageResult = instanceRepository.findByInitiatedByAndStatus(currentUser, status, pageable);
        } else if (templateId != null) {
            // Template filter only - FIXED: Use manual filtering with PageImpl
            List<WorkflowInstance> allByTemplate = instanceRepository.findByTemplateIdOrderByStartDateDesc(templateId);
            List<WorkflowInstance> userWorkflows = allByTemplate.stream()
                    .filter(workflow -> workflow.getInitiatedBy() != null && 
                                       workflow.getInitiatedBy().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
            
            // Create a Page from the filtered list
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), userWorkflows.size());
            List<WorkflowInstance> pageContent = userWorkflows.subList(start, end);
            
            pageResult = new PageImpl<>(pageContent, pageable, userWorkflows.size());
        } else if (from != null && to != null) {
            // Date range filter
            pageResult = instanceRepository.findByInitiatedByAndStartDateBetween(currentUser, from, to, pageable);
        } else {
            // No filters - get all user's workflows
            pageResult = instanceRepository.findByInitiatedBy(currentUser, pageable);
        }

        // Convert to DTOs
        List<WorkflowInstanceDTO> items = pageResult.getContent().stream()
                .map(WorkflowMapper::toInstanceDTO)
                .collect(Collectors.toList());

        // Build response
        Map<String, Object> response = buildPaginatedResponse(pageResult, items);
        
        log.debug("Returning {} workflows for user {}", items.size(), currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Search workflows by document name
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchWorkflows(
            @RequestParam String q,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Searching workflows with query: '{}'", q);

        if (q == null || q.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowInstance> pageResult = instanceRepository.searchByDocumentName(q.trim(), pageable);

        List<WorkflowInstanceDTO> items = pageResult.getContent().stream()
                .map(WorkflowMapper::toInstanceDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = buildPaginatedResponse(pageResult, items);
        
        log.debug("Found {} workflows matching query '{}'", items.size(), q);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all workflows (admin/manager only)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllWorkflows(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin/Manager fetching all workflows with filters");

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowInstance> pageResult;

        if (status != null) {
            pageResult = instanceRepository.findByStatus(status, pageable);
        } else if (templateId != null) {
            pageResult = instanceRepository.findByTemplateId(templateId, pageable);
        } else {
            pageResult = instanceRepository.findAll(pageable);
        }

        List<WorkflowInstanceDTO> items = pageResult.getContent().stream()
                .map(WorkflowMapper::toInstanceDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = buildPaginatedResponse(pageResult, items);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a workflow instance (initiator or MANAGER/ADMIN)
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelWorkflow(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason) {

        log.info("Cancelling workflow {} with reason: {}", id, reason);

        try {
            workflowService.cancelWorkflow(id, reason);
            
            Map<String, String> response = Map.of(
                "message", "Workflow cancelled successfully",
                "workflowId", id.toString(),
                "reason", reason != null ? reason : "No reason provided"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error cancelling workflow {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to cancel workflow");
        }
    }

    /**
     * Get workflow statistics (admin/manager only)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWorkflowStats() {
        log.debug("Fetching workflow statistics");

        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Count by status
            long totalWorkflows = instanceRepository.count();
            long inProgressCount = instanceRepository.countByStatus(WorkflowStatus.IN_PROGRESS);
            long approvedCount = instanceRepository.countByStatus(WorkflowStatus.APPROVED);
            long rejectedCount = instanceRepository.countByStatus(WorkflowStatus.REJECTED);
            long cancelledCount = instanceRepository.countByStatus(WorkflowStatus.CANCELLED);
            
            stats.put("total", totalWorkflows);
            stats.put("inProgress", inProgressCount);
            stats.put("approved", approvedCount);
            stats.put("rejected", rejectedCount);
            stats.put("cancelled", cancelledCount);
            
            // Recent activity (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentCount = instanceRepository.countCreatedSince(weekAgo);
            stats.put("recentlyCreated", recentCount);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching workflow statistics: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch statistics");
        }
    }

    // ===== HELPER METHODS =====

    private Map<String, Object> buildPaginatedResponse(Page<WorkflowInstance> pageResult, 
                                                      List<WorkflowInstanceDTO> items) {
        Map<String, Object> response = new HashMap<>();
        response.put("workflows", items);
        response.put("currentPage", pageResult.getNumber());
        response.put("pageSize", pageResult.getSize());
        response.put("totalItems", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("hasNext", pageResult.hasNext());
        response.put("hasPrevious", pageResult.hasPrevious());
        response.put("isFirst", pageResult.isFirst());
        response.put("isLast", pageResult.isLast());
        return response;
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username == null) {
                throw new ResponseStatusException(UNAUTHORIZED, "No authenticated user");
            }
            
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
                    
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication error");
        }
    }

    /**
     * FIXED: Now used for authorization - checks if user can access workflow
     */
    private boolean canAccessWorkflow(User user, WorkflowInstance workflow) {
        // User can access if they initiated it, are assigned to a task, or are admin/manager
        boolean isInitiator = workflow.getInitiatedBy() != null && 
                             workflow.getInitiatedBy().getId().equals(user.getId());
        
        boolean hasTask = workflow.getTasks() != null && 
                         workflow.getTasks().stream().anyMatch(task -> 
                             task.getAssignedTo() != null && 
                             task.getAssignedTo().getId().equals(user.getId()));
        
        boolean isAdminOrManager = user.getRole() == com.clouddocs.backend.entity.Role.ADMIN ||
                                  user.getRole() == com.clouddocs.backend.entity.Role.MANAGER;
        
        return isInitiator || hasTask || isAdminOrManager;
    }
}
