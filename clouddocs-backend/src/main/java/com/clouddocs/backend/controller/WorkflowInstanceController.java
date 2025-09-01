package com.clouddocs.backend.controller;


import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.WorkflowInstanceRepository;
import com.clouddocs.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
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
@RequestMapping("/workflows")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceRepository instanceRepository;
    private final UserRepository userRepository;

    /**
     * ✅ COMPLETELY REWRITTEN: Enhanced /mine endpoint with comprehensive fixes
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
            log.info("🔍 Fetching workflows for current user - status: {}, page: {}, size: {}", 
                    status, page, size);

            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            User currentUser = getCurrentUser(userPrincipal.getId());

            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // ✅ Use enhanced repository methods with JOIN FETCH
            Page<WorkflowInstance> pageResult = getWorkflowsWithFilters(
                currentUser, status, templateId, from, to, pageable);

            // ✅ CRITICAL FIX: Enhanced safe DTO conversion
            List<Map<String, Object>> workflowDTOs = pageResult.getContent().stream()
                    .map(this::convertToEnhancedWorkflowDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> response = buildPaginatedResponse(pageResult, workflowDTOs);
            
            log.info("✅ Returning {} workflows for user {}", 
                    workflowDTOs.size(), userPrincipal.getUsername());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting user workflows: {}", e.getMessage(), e);
            
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
     * ✅ ENHANCED: Safe DTO conversion with comprehensive error handling
     */
    private Map<String, Object> convertToEnhancedWorkflowDTO(WorkflowInstance workflow) {
        Map<String, Object> dto = new HashMap<>();
        
        try {
            // Basic workflow information
            dto.put("id", workflow.getId());
            dto.put("title", workflow.getTitle() != null ? workflow.getTitle() : "Document Approval");
            dto.put("description", workflow.getDescription());
            dto.put("status", workflow.getStatus().toString());
            dto.put("priority", workflow.getPriority() != null ? workflow.getPriority().toString() : "NORMAL");
            dto.put("currentStepOrder", workflow.getCurrentStepOrder());
            dto.put("comments", workflow.getComments());

            // ✅ Enhanced date handling
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dto.put("createdDate", workflow.getCreatedDate() != null ? 
                workflow.getCreatedDate().format(formatter) : LocalDateTime.now().format(formatter));
            dto.put("startDate", workflow.getStartDate() != null ? 
                workflow.getStartDate().format(formatter) : workflow.getCreatedDate().format(formatter));
            dto.put("updatedDate", workflow.getUpdatedDate() != null ? 
                workflow.getUpdatedDate().format(formatter) : workflow.getCreatedDate().format(formatter));
            dto.put("endDate", workflow.getEndDate() != null ? 
                workflow.getEndDate().format(formatter) : null);
            dto.put("dueDate", workflow.getDueDate() != null ? 
                workflow.getDueDate().format(formatter) : null);

            // ✅ Calculate and display relative time (fixes "5 hours ago" issue)
            LocalDateTime lastUpdate = workflow.getUpdatedDate() != null ? 
                workflow.getUpdatedDate() : workflow.getCreatedDate();
            dto.put("lastUpdatedRelative", calculateRelativeTime(lastUpdate));
            dto.put("lastUpdatedAbsolute", lastUpdate.format(formatter));

            // ✅ Safe template handling
            handleTemplateInfo(workflow, dto);

            // ✅ Safe initiator handling (fixes "Unassigned" issue)
            handleInitiatorInfo(workflow, dto);

            // ✅ Safe document handling
            handleDocumentInfo(workflow, dto);

            // ✅ Enhanced task handling (fixes missing approve/reject options)
            handleTasksInfo(workflow, dto);

            return dto;
            
        } catch (Exception e) {
            log.error("❌ Error converting workflow {} to enhanced DTO: {}", 
                    workflow != null ? workflow.getId() : "null", e.getMessage());
            
            // Return minimal safe DTO
            return createMinimalWorkflowDTO(workflow);
        }
    }

    private void handleTemplateInfo(WorkflowInstance workflow, Map<String, Object> dto) {
        try {
            if (workflow.getTemplate() != null) {
                dto.put("templateName", workflow.getTemplate().getName());
                dto.put("templateId", workflow.getTemplate().getId());
                dto.put("templateType", workflow.getTemplate().getType() != null ? 
                    workflow.getTemplate().getType().toString() : "UNKNOWN");
            } else {
                dto.put("templateName", "No Template");
                dto.put("templateId", null);
                dto.put("templateType", "UNKNOWN");
            }
        } catch (Exception e) {
            log.debug("⚠️ Could not load template for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.put("templateName", "Template Load Error");
            dto.put("templateId", null);
            dto.put("templateType", "UNKNOWN");
        }
    }

    private void handleInitiatorInfo(WorkflowInstance workflow, Map<String, Object> dto) {
        try {
            if (workflow.getInitiatedBy() != null) {
                String fullName = workflow.getInitiatedBy().getFullName();
                dto.put("initiatedByName", fullName != null && !fullName.trim().isEmpty() ? 
                    fullName : workflow.getInitiatedBy().getUsername());
                dto.put("initiatedById", workflow.getInitiatedBy().getId());
                dto.put("assignedTo", fullName != null && !fullName.trim().isEmpty() ? 
                    fullName : workflow.getInitiatedBy().getUsername()); // ✅ Fix "Unassigned"
            } else {
                dto.put("initiatedByName", "System");
                dto.put("initiatedById", null);
                dto.put("assignedTo", "System");
            }
        } catch (Exception e) {
            log.debug("⚠️ Could not load initiator for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.put("initiatedByName", "Unknown User");
            dto.put("initiatedById", null);
            dto.put("assignedTo", "Unknown User");
        }
    }

    private void handleDocumentInfo(WorkflowInstance workflow, Map<String, Object> dto) {
        try {
            if (workflow.getDocument() != null) {
                String docName = workflow.getDocument().getOriginalFilename() != null ?
                    workflow.getDocument().getOriginalFilename() : workflow.getDocument().getFilename();
                dto.put("documentName", docName != null ? docName : "Unknown Document");
                dto.put("documentId", workflow.getDocument().getId());
            } else {
                dto.put("documentName", "No Document");
                dto.put("documentId", null);
            }
        } catch (Exception e) {
            log.debug("⚠️ Could not load document for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.put("documentName", "Document Load Error");
            dto.put("documentId", null);
        }
    }

    private void handleTasksInfo(WorkflowInstance workflow, Map<String, Object> dto) {
        try {
            if (workflow.getTasks() != null && !workflow.getTasks().isEmpty()) {
                List<Map<String, Object>> taskDTOs = new ArrayList<>();
                
                int totalTasks = workflow.getTasks().size();
                int completedTasks = 0;
                int pendingTasks = 0;
                
                for (WorkflowTask task : workflow.getTasks()) {
                    Map<String, Object> taskDto = createTaskDTO(task);
                    taskDTOs.add(taskDto);
                    
                    if ("COMPLETED".equals(task.getStatus().toString()) || 
                        "APPROVED".equals(task.getStatus().toString())) {
                        completedTasks++;
                    } else if ("PENDING".equals(task.getStatus().toString())) {
                        pendingTasks++;
                    }
                }
                
                dto.put("tasks", taskDTOs);
                dto.put("totalTasks", totalTasks);
                dto.put("completedTasks", completedTasks);
                dto.put("pendingTasks", pendingTasks);
                dto.put("progress", totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0);
                
                // ✅ Fix assignedTo based on current pending task
                String currentAssignee = findCurrentAssignee(workflow.getTasks());
                if (currentAssignee != null) {
                    dto.put("assignedTo", currentAssignee);
                }
                
            } else {
                dto.put("tasks", new ArrayList<>());
                dto.put("totalTasks", 0);
                dto.put("completedTasks", 0);
                dto.put("pendingTasks", 0);
                dto.put("progress", 0);
            }
        } catch (Exception e) {
            log.debug("⚠️ Could not load tasks for workflow {}: {}", workflow.getId(), e.getMessage());
            dto.put("tasks", new ArrayList<>());
            dto.put("totalTasks", 0);
            dto.put("completedTasks", 0);
            dto.put("pendingTasks", 0);
            dto.put("progress", 0);
        }
    }

    private Map<String, Object> createTaskDTO(WorkflowTask task) {
        Map<String, Object> taskDto = new HashMap<>();
        
        try {
            taskDto.put("id", task.getId());
            taskDto.put("status", task.getStatus().toString());
            taskDto.put("comments", task.getComments());
            taskDto.put("createdAt", task.getCreatedAt() != null ? 
                task.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            taskDto.put("completedAt", task.getCompletedAt() != null ? 
                task.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            
            // Safe assigned user handling
            try {
                if (task.getAssignedTo() != null) {
                    String assignedName = task.getAssignedTo().getFullName();
                    taskDto.put("assignedTo", assignedName != null && !assignedName.trim().isEmpty() ? 
                        assignedName : task.getAssignedTo().getUsername());
                    taskDto.put("assignedToId", task.getAssignedTo().getId());
                } else {
                    taskDto.put("assignedTo", "Unassigned");
                    taskDto.put("assignedToId", null);
                }
            } catch (Exception e) {
                taskDto.put("assignedTo", "Assignment Error");
                taskDto.put("assignedToId", null);
            }
            
            // Safe step handling
            try {
                if (task.getStep() != null) {
                    taskDto.put("stepName", task.getStep().getName());
                    taskDto.put("stepOrder", task.getStep().getStepOrder());
                    taskDto.put("stepType", task.getStep().getType().toString());
                    taskDto.put("canApprove", task.getStep().getType() == StepType.APPROVAL);
                    taskDto.put("canReject", task.getStep().getType() == StepType.APPROVAL);
                } else {
                    taskDto.put("stepName", "Unknown Step");
                    taskDto.put("stepOrder", 0);
                    taskDto.put("stepType", "UNKNOWN");
                    taskDto.put("canApprove", false);
                    taskDto.put("canReject", false);
                }
            } catch (Exception e) {
                taskDto.put("stepName", "Step Load Error");
                taskDto.put("stepOrder", 0);
                taskDto.put("stepType", "UNKNOWN");
                taskDto.put("canApprove", false);
                taskDto.put("canReject", false);
            }
            
        } catch (Exception e) {
            log.warn("Error creating task DTO for task {}: {}", task.getId(), e.getMessage());
        }
        
        return taskDto;
    }

    private String findCurrentAssignee(List<WorkflowTask> tasks) {
        try {
            // Find the first pending task and get its assignee
            for (WorkflowTask task : tasks) {
                if ("PENDING".equals(task.getStatus().toString()) && task.getAssignedTo() != null) {
                    String fullName = task.getAssignedTo().getFullName();
                    return fullName != null && !fullName.trim().isEmpty() ? 
                        fullName : task.getAssignedTo().getUsername();
                }
            }
        } catch (Exception e) {
            log.debug("Error finding current assignee: {}", e.getMessage());
        }
        return null;
    }

    private String calculateRelativeTime(LocalDateTime dateTime) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(dateTime, now).toMinutes();
            
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " minutes ago";
            
            long hours = minutes / 60;
            if (hours < 24) return hours + " hours ago";
            
            long days = hours / 24;
            if (days < 7) return days + " days ago";
            
            long weeks = days / 7;
            if (weeks < 4) return weeks + " weeks ago";
            
            long months = days / 30;
            return months + " months ago";
            
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private Map<String, Object> createMinimalWorkflowDTO(WorkflowInstance workflow) {
        Map<String, Object> minimal = new HashMap<>();
        try {
            minimal.put("id", workflow.getId());
            minimal.put("status", workflow.getStatus().toString());
            minimal.put("title", workflow.getTitle() != null ? workflow.getTitle() : "Workflow");
            minimal.put("assignedTo", "Error Loading");
            minimal.put("lastUpdatedRelative", "Unknown");
            minimal.put("tasks", new ArrayList<>());
            minimal.put("totalTasks", 0);
            minimal.put("completedTasks", 0);
        } catch (Exception e) {
            log.error("Error creating minimal DTO: {}", e.getMessage());
        }
        return minimal;
    }

    // ===== ENHANCED SUPPORTING METHODS =====

    private Page<WorkflowInstance> getWorkflowsWithFilters(User currentUser, String status, 
                                                          UUID templateId, LocalDateTime from, 
                                                          LocalDateTime to, Pageable pageable) {
        try {
            WorkflowStatus workflowStatus = status != null && !status.equals("All Statuses") ? 
                WorkflowStatus.valueOf(status.toUpperCase()) : null;
            
            if (workflowStatus != null && templateId != null) {
                return instanceRepository.findByInitiatedByAndStatusAndTemplateIdWithDetails(
                    currentUser, workflowStatus, templateId, pageable);
            } else if (workflowStatus != null) {
                return instanceRepository.findByInitiatedByAndStatusWithDetails(
                    currentUser, workflowStatus, pageable);
            } else if (templateId != null) {
                return instanceRepository.findByInitiatedByAndTemplateIdWithDetails(
                    currentUser, templateId, pageable);
            } else if (from != null && to != null) {
                return instanceRepository.findByInitiatedByAndCreatedDateBetweenWithDetails(
                    currentUser, from, to, pageable);
            } else {
                return instanceRepository.findByInitiatedByWithDetailsOrderByCreatedDateDesc(
                    currentUser, pageable);
            }
        } catch (Exception e) {
            log.error("Error getting workflows with filters: {}", e.getMessage());
            return Page.empty(pageable);
        }
    }

    private Map<String, Object> buildPaginatedResponse(Page<?> pageResult, List<?> items) {
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

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    private User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // ===== ADDITIONAL ENDPOINTS (keeping your existing ones) =====

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getWorkflow(@PathVariable Long id) {
        try {
            log.debug("Fetching workflow instance with ID: {}", id);
            
            WorkflowInstance instance = instanceRepository.findByIdWithBasicDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            User currentUser = getCurrentUser(userPrincipal.getId());
            
            if (!canAccessWorkflow(currentUser, instance)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this workflow");
            }
            
            Map<String, Object> dto = convertToEnhancedWorkflowDTO(instance);
            return ResponseEntity.ok(dto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error getting workflow {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found");
        }
    }

    private boolean canAccessWorkflow(User user, WorkflowInstance workflow) {
        try {
            boolean isInitiator = workflow.getInitiatedBy() != null && 
                                 workflow.getInitiatedBy().getId().equals(user.getId());
            
            boolean hasTask = workflow.getTasks() != null && 
                             workflow.getTasks().stream().anyMatch(task -> 
                                 task.getAssignedTo() != null && 
                                 task.getAssignedTo().getId().equals(user.getId()));
            
            boolean isAdminOrManager = user.getRole() == Role.ADMIN ||
                                      user.getRole() == Role.MANAGER;
            
            return isInitiator || hasTask || isAdminOrManager;
        } catch (Exception e) {
            log.error("Error checking workflow access: {}", e.getMessage());
            return false;
        }
    }
}
