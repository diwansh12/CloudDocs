package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowInstance;
import com.clouddocs.backend.entity.WorkflowStatus;
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
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequestMapping("/workflows")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowService workflowService;
    private final UserRepository userRepository;

    /**
     * ✅ MAIN FIX: Enhanced /mine endpoint with safe DTO conversion
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    @Transactional(readOnly = true)  // ✅ CRITICAL: Ensure session stays active
    public ResponseEntity<Map<String, Object>> getMyWorkflows(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            log.info("🔍 Fetching workflows for current user with filters - status: {}, templateId: {}", status, templateId);

            User currentUser = getCurrentUser();

            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<WorkflowInstance> pageResult;

            // ✅ Use enhanced repository methods with JOIN FETCH
            if (status != null && templateId != null) {
                pageResult = instanceRepository.findByInitiatedByAndStatusAndTemplateIdWithDetails(currentUser, status, templateId, pageable);
            } else if (status != null) {
                pageResult = instanceRepository.findByInitiatedByAndStatusWithDetails(currentUser, status, pageable);
            } else if (templateId != null) {
                pageResult = instanceRepository.findByInitiatedByAndTemplateIdWithDetails(currentUser, templateId, pageable);
            } else if (from != null && to != null) {
                pageResult = instanceRepository.findByInitiatedByAndStartDateBetweenWithDetails(currentUser, from, to, pageable);
            } else {
                pageResult = instanceRepository.findByInitiatedByWithDetails(currentUser, pageable);
            }

            // ✅ CRITICAL FIX: Use safe DTO conversion instead of direct mapping
            List<WorkflowInstanceDTO> items = pageResult.getContent().stream()
                    .map(this::convertToWorkflowInstanceDTOSafe)  // Safe conversion
                    .filter(dto -> dto != null)  // Remove failed conversions
                    .collect(Collectors.toList());

            Map<String, Object> response = buildPaginatedResponse(pageResult, items);
            
            log.info("✅ Returning {} workflows for user {}", items.size(), currentUser.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting user workflows: {}", e.getMessage(), e);
            
            // ✅ Return empty response instead of throwing exception
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("workflows", new ArrayList<>());
            emptyResponse.put("currentPage", page);
            emptyResponse.put("totalItems", 0L);
            emptyResponse.put("totalPages", 0);
            emptyResponse.put("hasNext", false);
            emptyResponse.put("hasPrevious", false);
            
            return ResponseEntity.ok(emptyResponse);
        }
    }

    /**
     * ✅ SAFE DTO conversion method with comprehensive error handling
     */
   private WorkflowInstanceDTO convertToWorkflowInstanceDTOSafe(WorkflowInstance workflow) {
    try {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        
        // ✅ Basic fields matching your DTO
        dto.setId(workflow.getId());
        dto.setTitle(workflow.getTitle()); // Now works with added field
        dto.setDescription(workflow.getDescription()); // Now works with added field
        dto.setStatus(workflow.getStatus()); // Enum type - perfect!
        dto.setCurrentStepOrder(workflow.getCurrentStepOrder());
        dto.setPriority(workflow.getPriority()); // WorkflowPriority enum
        dto.setComments(workflow.getComments());
        
        // ✅ Date fields matching your DTO naming
        dto.setStartDate(workflow.getStartDate());
        dto.setEndDate(workflow.getEndDate());
        dto.setDueDate(workflow.getDueDate());
         dto.setCreatedDate(workflow.getCreatedDate());  // ✅ Now works!
        dto.setUpdatedDate(workflow.getUpdatedDate());  // Map updatedAt -> updatedDate
        
        // ✅ Safe handling of lazy-loaded template relationship
        try {
            if (workflow.getTemplate() != null) {
                dto.setTemplateName(workflow.getTemplate().getName());
                dto.setTemplateId(workflow.getTemplate().getId());
            } else {
                dto.setTemplateName("Unknown Template");
                dto.setTemplateId(null);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not load template for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.setTemplateName("Unknown Template");
            dto.setTemplateId(null);
        }
        
        // ✅ Safe handling of lazy-loaded initiatedBy relationship
        try {
            if (workflow.getInitiatedBy() != null) {
                dto.setInitiatedByName(workflow.getInitiatedBy().getFullName());
                dto.setInitiatedById(workflow.getInitiatedBy().getId());
            } else {
                dto.setInitiatedByName("Unknown User");
                dto.setInitiatedById(null);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not load initiator for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.setInitiatedByName("Unknown User");
            dto.setInitiatedById(null);
        }
        
        // ✅ Safe handling of lazy-loaded document relationship
        try {
            if (workflow.getDocument() != null) {
                dto.setDocumentName(workflow.getDocument().getOriginalFilename());
                dto.setDocumentId(workflow.getDocument().getId());
            } else {
                dto.setDocumentName("Unknown Document");
                dto.setDocumentId(null);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not load document for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.setDocumentName("Unknown Document");
            dto.setDocumentId(null);
        }
        
        // ✅ Safe handling of lazy-loaded tasks collection
        try {
            if (workflow.getTasks() != null && !workflow.getTasks().isEmpty()) {
                dto.setTotalTasks(workflow.getTasks().size());
                long completedTasks = workflow.getTasks().stream()
                        .mapToLong(task -> "COMPLETED".equals(task.getStatus().toString()) ? 1 : 0)
                        .sum();
                dto.setCompletedTasks((int) completedTasks);
                
                // ✅ Optional: Convert full task details if needed
                // dto.setTasks(workflow.getTasks().stream()
                //         .map(this::convertTaskToDTO)
                //         .collect(Collectors.toList()));
            } else {
                dto.setTotalTasks(0);
                dto.setCompletedTasks(0);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not load tasks for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.setTotalTasks(0);
            dto.setCompletedTasks(0);
        }
        
        return dto;
        
    } catch (Exception e) {
        log.error("❌ Error converting workflow {} to DTO: {}", 
                    workflow != null ? workflow.getId() : "null", e.getMessage());
        return null;  // This will be filtered out in the stream
    }
}

    /**
     * ✅ ENHANCED: Other endpoints with same safe conversion pattern
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowInstanceDTO> getWorkflow(@PathVariable Long id) {
        try {
            log.debug("Fetching workflow instance with ID: {}", id);
            
            WorkflowInstance instance = instanceRepository.findByIdWithBasicDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
            
            User currentUser = getCurrentUser();
            if (!canAccessWorkflow(currentUser, instance)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this workflow");
            }
            
            // ✅ Use safe conversion instead of direct mapping
            WorkflowInstanceDTO dto = convertToWorkflowInstanceDTOSafe(instance);
            if (dto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow could not be processed");
            }
            
            return ResponseEntity.ok(dto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting workflow {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found");
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/details")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowWithTasks(@PathVariable Long id) {
        try {
            log.debug("Fetching workflow instance with tasks for ID: {}", id);
            
            WorkflowInstance instance = instanceRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
            
            User currentUser = getCurrentUser();
            if (!canAccessWorkflow(currentUser, instance)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this workflow");
            }
            
            // ✅ Use safe conversion
            WorkflowInstanceDTO dto = convertToWorkflowInstanceDTOSafe(instance);
            if (dto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow could not be processed");
            }
            
            return ResponseEntity.ok(dto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting workflow with tasks {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found");
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> searchWorkflows(
            @RequestParam String q,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            log.debug("Searching workflows with query: '{}'", q);

            if (q == null || q.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
            }

            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<WorkflowInstance> pageResult = instanceRepository.searchByDocumentNameWithDetails(q.trim(), pageable);

            // ✅ Use safe conversion
            List<WorkflowInstanceDTO> items = pageResult.getContent().stream()
                    .map(this::convertToWorkflowInstanceDTOSafe)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            Map<String, Object> response = buildPaginatedResponse(pageResult, items);
            
            log.debug("Found {} workflows matching query '{}'", items.size(), q);
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error searching workflows: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("workflows", new ArrayList<>());
            emptyResponse.put("currentPage", page);
            emptyResponse.put("totalItems", 0L);
            emptyResponse.put("totalPages", 0);
            
            return ResponseEntity.ok(emptyResponse);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/all")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllWorkflows(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            log.debug("Admin/Manager fetching all workflows with filters");

            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<WorkflowInstance> pageResult;

            if (status != null) {
                pageResult = instanceRepository.findByStatusWithDetails(status, pageable);
            } else if (templateId != null) {
                pageResult = instanceRepository.findByTemplateIdWithDetails(templateId, pageable);
            } else {
                pageResult = instanceRepository.findAllWithDetails(pageable);
            }

            // ✅ Use safe conversion
            List<WorkflowInstanceDTO> items = pageResult.getContent().stream()
                    .map(this::convertToWorkflowInstanceDTOSafe)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            Map<String, Object> response = buildPaginatedResponse(pageResult, items);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting all workflows: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("workflows", new ArrayList<>());
            emptyResponse.put("currentPage", page);
            emptyResponse.put("totalItems", 0L);
            emptyResponse.put("totalPages", 0);
            
            return ResponseEntity.ok(emptyResponse);
        }
    }

    // ✅ Keep all your existing methods with same safe pattern applied
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/cancel")
    @Transactional
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

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getWorkflowStats() {
        try {
            log.debug("Fetching workflow statistics");

            Map<String, Object> stats = new HashMap<>();
            
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
            
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentCount = instanceRepository.countCreatedSince(weekAgo);
            stats.put("recentlyCreated", recentCount);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching workflow statistics: {}", e.getMessage(), e);
            
            // Return empty stats instead of throwing exception
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("total", 0L);
            emptyStats.put("inProgress", 0L);
            emptyStats.put("approved", 0L);
            emptyStats.put("rejected", 0L);
            emptyStats.put("cancelled", 0L);
            emptyStats.put("recentlyCreated", 0L);
            
            return ResponseEntity.ok(emptyStats);
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

    private boolean canAccessWorkflow(User user, WorkflowInstance workflow) {
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
