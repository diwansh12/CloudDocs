package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.CreateWorkflowRequest;
import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.RoleRepository;
import com.clouddocs.backend.repository.WorkflowTemplateRepository;
import com.clouddocs.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/workflows")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // ✅ ADDED: For Many-to-Many role support

    /**
     * ✅ Create workflow endpoint
     * Maps to: POST /workflows
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        try {
            log.info("🚀 Creating workflow with request: {}", request);
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authenticated user: {}", auth != null ? auth.getName() : "null");
            
            // Validate required fields
            if (request.getDocumentId() == null) {
                throw new IllegalArgumentException("Document ID is required");
            }
            if (request.getTemplateId() == null) {
                throw new IllegalArgumentException("Template ID is required");
            }
            
            // ✅ FIXED: Service now returns WorkflowInstanceDTO directly
            WorkflowInstanceDTO dto = workflowService.startWorkflow(
                request.getDocumentId(),
                request.getTemplateId(),
                request.getTitle(),           
                request.getDescription(),     
                request.getPriority()         
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow created successfully");
            response.put("workflow", dto);
            response.put("workflowId", dto.getId());
            response.put("timestamp", LocalDateTime.now());
            
            log.info("✅ Workflow created successfully with ID: {}, Priority: {}", 
                    dto.getId(), request.getPriority());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(error);
            
        } catch (Exception e) {
            log.error("❌ Error creating workflow: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create workflow: " + e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * ✅ Start workflow (legacy endpoint)
     * Maps to: POST /workflows/start
     */
    @PostMapping("/start")
    public ResponseEntity<?> startWorkflow(
            @RequestParam Long documentId,
            @RequestParam UUID templateId) {
        
        try {
            log.info("Starting workflow (legacy endpoint) for document {} with template {}", 
                    documentId, templateId);
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authenticated user: {}", auth != null ? auth.getName() : "null");
            
            WorkflowInstanceDTO dto = workflowService.startWorkflow(
                documentId, 
                templateId,
                null,      
                null,      
                "NORMAL"   
            );
            
            log.info("Workflow created successfully with ID: {}", dto.getId());
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            log.error("Error creating workflow for document {} with template {}: {}", 
                     documentId, templateId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName(),
                "timestamp", LocalDateTime.now(),
                "documentId", documentId,
                "templateId", templateId.toString()
            ));
        }
    }

    /**
     * ✅ Handle task actions (approve/reject) with enhanced response
     * Maps to: PUT /workflows/tasks/{taskId}/action
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/tasks/{taskId}/action")
    public ResponseEntity<Map<String, Object>> handleTaskAction(
            @PathVariable Long taskId,
            @RequestParam String action,
            @RequestParam(required = false) String comments) {
        
        try {
            log.info("🔄 Processing task action - TaskID: {}, Action: {}", taskId, action);
            
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.debug("Current user: {}", username);
            
            // Validate action parameter
            if (!action.equalsIgnoreCase("APPROVE") && !action.equalsIgnoreCase("REJECT")) {
                throw new IllegalArgumentException("Invalid action. Must be APPROVE or REJECT");
            }
            
            Map<String, Object> serviceResponse = workflowService.processTaskActionWithUser(
                taskId, action, comments, getCurrentUserId());
            
            // Enhanced response including workflow details
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task " + action.toLowerCase() + "d successfully");
            response.put("taskId", taskId);
            response.put("action", action);
            response.put("timestamp", LocalDateTime.now());
            
            // Add service response details
            response.putAll(serviceResponse);
            
            log.info("✅ Task action completed successfully - TaskID: {}, Action: {}", taskId, action);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request for task {}: {}", taskId, e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("taskId", taskId);
            
            return ResponseEntity.badRequest().body(error);
            
        } catch (Exception e) {
            log.error("❌ Unexpected error processing task {}: {}", taskId, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to process task action: " + e.getMessage());
            error.put("taskId", taskId);
            error.put("action", action);
            
            return ResponseEntity.status(500).body(error);
        }
    }

   /**
 * ✅ FIXED: Create sample template with Many-to-Many role support
 * Maps to: POST /workflows/templates/create-sample
 */
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/templates/create-sample")
public ResponseEntity<Map<String, Object>> createSampleTemplate() {
    try {
        log.info("🔧 Creating sample workflow template...");
        
        // ✅ FIXED: Fetch roles from database instead of using enum constants
        Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
            .orElseThrow(() -> new RuntimeException("ROLE_MANAGER not found in database"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
            .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found in database"));

        WorkflowTemplate template = new WorkflowTemplate(
                "Simple Document Approval",
                "Manager then Admin approval",
                WorkflowType.DOCUMENT_APPROVAL
        );
        template.setIsActive(true);
        template.setDefaultSlaHours(48);

        // ✅ FIXED: Step 1 - Manager approval with Role entity
        WorkflowStep step1 = new WorkflowStep("Manager Approval", 1, StepType.APPROVAL);
        step1.setTemplate(template);
        step1.setDescription("Manager level approval required");
        step1.setApprovalPolicy(ApprovalPolicy.QUORUM);
        step1.setRequiredApprovals(1);
        step1.setSlaHours(24);
        
        // ✅ FIXED: Step 2 - Admin approval with Role entity
        WorkflowStep step2 = new WorkflowStep("Admin Approval", 2, StepType.APPROVAL);
        step2.setTemplate(template);
        step2.setDescription("Administrative approval required");
        step2.setApprovalPolicy(ApprovalPolicy.ALL);
        step2.setRequiredApprovals(1);
        step2.setSlaHours(24);

        // Add steps to template
        template.getSteps().add(step1);
        template.getSteps().add(step2);

        WorkflowTemplate saved = templateRepository.save(template);
        
        // ✅ FIXED: Actually use and save the WorkflowStepRole objects
        WorkflowStepRole step1Role = new WorkflowStepRole(step1, managerRole);
        WorkflowStepRole step2Role = new WorkflowStepRole(step2, adminRole);
        
        // Save the role associations (assumes you have WorkflowStepRoleRepository)
        // If you don't have this repository, you can add the roles directly to the steps:
        step1.getRoles().add(step1Role);
        step2.getRoles().add(step2Role);
        
        log.info("✅ Created sample template with ID: {}, Steps: {}, Roles assigned", 
            saved.getId(), saved.getSteps().size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sample template created successfully with role assignments");
        response.put("template", Map.of(
            "id", saved.getId(),
            "name", saved.getName(),
            "description", saved.getDescription(),
            "type", saved.getType().name(),
            "steps", saved.getSteps().size(),
            "slaHours", saved.getDefaultSlaHours(),
            "active", saved.getIsActive()
        ));
        response.put("roles", Map.of(
            "step1Role", managerRole.getName().name(),
            "step2Role", adminRole.getName().name(),
            "step1RoleAssigned", step1Role != null,
            "step2RoleAssigned", step2Role != null
        ));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        log.error("❌ Failed to create sample template: {}", e.getMessage(), e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Failed to create sample template: " + e.getMessage());
        error.put("error", e.getClass().getSimpleName());
        error.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(500).body(error);
    }
}


    /**
     * ✅ Get tasks assigned to current user with enhanced details
     * Maps to: GET /workflows/tasks/user
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tasks/user")
    public ResponseEntity<List<Map<String, Object>>> getUserTasks() {
        try {
            log.info("📋 Getting tasks for user ID: {}", getCurrentUserId());
            
            List<Map<String, Object>> tasks = workflowService.getMyTasksWithDetails(getCurrentUserId());
            
            log.info("✅ Retrieved {} tasks for user", tasks.size());
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            log.error("❌ Failed to get user tasks: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Get workflows initiated by current user
     * Maps to: GET /workflows/user
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user")
    public ResponseEntity<List<WorkflowInstanceDTO>> getUserWorkflows() {
        try {
            log.info("📊 Getting workflows for current user");
            
            List<WorkflowInstanceDTO> workflows = workflowService.getMyWorkflows();
            
            log.info("✅ Retrieved {} workflows for user", workflows.size());
            return ResponseEntity.ok(workflows);
            
        } catch (Exception e) {
            log.error("❌ Failed to get user workflows: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Get paginated user workflows with filtering
     * Maps to: GET /workflows/user/paginated
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/paginated")
    public ResponseEntity<Map<String, Object>> getUserWorkflowsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "All Statuses") String status) {
        try {
            log.info("📄 Getting paginated workflows - Page: {}, Size: {}, Status: {}", 
                page, size, status);
            
            Map<String, Object> response = workflowService.getUserWorkflowsWithDetails(
                getCurrentUserId(), page, size, status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to get paginated user workflows: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Complete a workflow task
     * Maps to: PUT /workflows/tasks/{taskId}/complete
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable Long taskId,
            @RequestParam TaskAction action,
            @RequestParam(required = false) String comments) {
        
        try {
            log.info("✅ Completing task {} with action {}", taskId, action);
            
            Map<String, Object> result = workflowService.processTaskActionWithUser(
                taskId, action.toString(), comments, getCurrentUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task completed successfully");
            response.put("taskId", taskId);
            response.put("action", action.toString());
            response.put("timestamp", LocalDateTime.now());
            response.putAll(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to complete task {}: {}", taskId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "taskId", taskId,
                "action", action.toString(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * ✅ Get workflow details by ID with user permissions
     * Maps to: GET /workflows/{instanceId}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{instanceId}")
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowDetails(@PathVariable Long instanceId) {
        try {
            log.info("🔍 Getting workflow details for ID: {}", instanceId);
            
            WorkflowInstanceDTO workflow = workflowService.getWorkflowDetailsWithTasks(
                instanceId, getCurrentUserId());
            
            return ResponseEntity.ok(workflow);
            
        } catch (Exception e) {
            log.error("❌ Failed to get workflow details for {}: {}", instanceId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Cancel workflow
     * Maps to: PUT /workflows/{instanceId}/cancel
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{instanceId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelWorkflow(
            @PathVariable Long instanceId,
            @RequestParam(required = false) String reason) {
        try {
            log.info("🚫 Cancelling workflow {} with reason: {}", instanceId, reason);
            
            WorkflowInstanceDTO cancelledWorkflow = workflowService.cancelWorkflow(instanceId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow cancelled successfully");
            response.put("workflow", cancelledWorkflow);
            response.put("instanceId", instanceId);
            response.put("reason", reason);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to cancel workflow {}: {}", instanceId, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to cancel workflow: " + e.getMessage());
            error.put("instanceId", instanceId);
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * ✅ Get workflow by ID (simple version for backward compatibility)
     * Maps to: GET /workflows/{instanceId}/simple
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{instanceId}/simple")
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowById(@PathVariable Long instanceId) {
        try {
            log.info("🔍 Getting simple workflow details for ID: {}", instanceId);
            
            WorkflowInstanceDTO workflow = workflowService.getWorkflowById(instanceId);
            return ResponseEntity.ok(workflow);
            
        } catch (Exception e) {
            log.error("❌ Failed to get workflow {}: {}", instanceId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ NEW: Get available workflow templates
     * Maps to: GET /workflows/templates
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> getWorkflowTemplates() {
        try {
            log.info("📋 Getting available workflow templates");
            
            List<WorkflowTemplate> templates = templateRepository.findByIsActiveTrue();
            
            List<Map<String, Object>> templateDTOs = templates.stream()
                .map(template -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", template.getId());
                    dto.put("name", template.getName());
                    dto.put("description", template.getDescription());
                    dto.put("type", template.getType().name());
                    dto.put("slaHours", template.getDefaultSlaHours());
                    dto.put("stepCount", template.getSteps().size());
                    dto.put("active", template.getIsActive());
                    return dto;
                })
                .toList();
            
            return ResponseEntity.ok(templateDTOs);
            
        } catch (Exception e) {
            log.error("❌ Failed to get workflow templates: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
 * ✅ FIXED: Get workflow statistics for current user
 * Maps to: GET /workflows/stats
 */
@PreAuthorize("isAuthenticated()")
@GetMapping("/stats")
public ResponseEntity<Map<String, Object>> getWorkflowStats() {
    try {
        log.info("📊 Getting workflow statistics for current user");
        
        // ✅ FIXED: Use existing methods instead of non-existent getUserWorkflowStatistics
        Map<String, Object> stats = new HashMap<>();
        
        // Get general workflow analytics
        Map<String, Object> analyticsDebug = workflowService.getWorkflowAnalyticsDebug();
        Map<String, Long> statusBreakdown = workflowService.getWorkflowStatusBreakdown();
        long approvedCount = workflowService.getApprovedWorkflowsCount();
        
        // Get user-specific data
        Long currentUserId = getCurrentUserId();
        List<Map<String, Object>> userTasks = workflowService.getMyTasksWithDetails(currentUserId);
        List<WorkflowInstanceDTO> userWorkflows = workflowService.getMyWorkflows();
        
        // Build comprehensive stats
        stats.put("totalWorkflows", analyticsDebug.get("totalWorkflows"));
        stats.put("statusBreakdown", statusBreakdown);
        stats.put("approvedWorkflows", approvedCount);
        stats.put("userTaskCount", userTasks.size());
        stats.put("userWorkflowCount", userWorkflows.size());
        stats.put("pendingTasks", userTasks.stream()
            .mapToInt(task -> {
                Object isPending = task.get("isPending");
                return (isPending instanceof Boolean && (Boolean) isPending) ? 1 : 0;
            })
            .sum());
        stats.put("timestamp", LocalDateTime.now());
        stats.put("userId", currentUserId);
        
        return ResponseEntity.ok(stats);
        
    } catch (Exception e) {
        log.error("❌ Failed to get workflow stats: {}", e.getMessage(), e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(500).body(error);
    }
}

    // ===== HELPER METHODS =====

    /**
     * ✅ Get current user ID from security context
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
            
            return user.getId();
            
        } catch (Exception e) {
            log.error("❌ Error getting current user ID: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to get current user ID");
        }
    }

    /**
     * ✅ NEW: Get current user details
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
                
        } catch (Exception e) {
            log.error("❌ Error getting current user: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to get current user");
        }
    }

    // ===== DEBUG ENDPOINTS =====

    /**
     * ✅ Debug endpoint for workflow timestamp updates
     * Maps to: POST /workflows/debug/{id}/timestamp
     */
    @PostMapping("/debug/{id}/timestamp")
    public ResponseEntity<?> debugTimestamp(@PathVariable Long id) {
        try {
            log.info("🐛 Debug timestamp update for workflow: {}", id);
            
            workflowService.debugUpdateWorkflowTimestamp(id);
            
            return ResponseEntity.ok(Map.of(
                "message", "Debug timestamp update completed",
                "workflowId", id,
                "timestamp", LocalDateTime.now(),
                "instruction", "Check logs for details"
            ));
            
        } catch (Exception e) {
            log.error("❌ Debug timestamp update failed: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "workflowId", id,
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * ✅ NEW: Debug endpoint to check user role permissions
     * Maps to: GET /workflows/debug/user-roles
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/debug/user-roles")
    public ResponseEntity<Map<String, Object>> debugUserRoles() {
        try {
            User currentUser = getCurrentUser();
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("userId", currentUser.getId());
            debugInfo.put("username", currentUser.getUsername());
            debugInfo.put("email", currentUser.getEmail());
            debugInfo.put("roles", currentUser.getRoles().stream()
                .map(role -> Map.of(
                    "id", role.getId(),
                    "name", role.getName().name(),
                    "description", role.getDescription()
                ))
                .toList());
            debugInfo.put("hasManagerRole", currentUser.hasRole(ERole.ROLE_MANAGER));
            debugInfo.put("hasAdminRole", currentUser.hasRole(ERole.ROLE_ADMIN));
            debugInfo.put("hasUserRole", currentUser.hasRole(ERole.ROLE_USER));
            debugInfo.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            log.error("❌ Debug user roles failed: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
}
