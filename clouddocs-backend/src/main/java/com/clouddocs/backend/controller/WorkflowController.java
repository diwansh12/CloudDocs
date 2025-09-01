package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.CreateWorkflowRequest;
import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.mapper.WorkflowMapper;
import com.clouddocs.backend.repository.WorkflowTemplateRepository;
import com.clouddocs.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/workflows")  // ✅ Base path for general workflow operations
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowTemplateRepository templateRepository;

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
                request.getTitle(),           // ✅ Include title
                request.getDescription(),     // ✅ Include description  
                request.getPriority()         // ✅ Include priority
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
            
            // ✅ FIXED: Service now returns WorkflowInstanceDTO directly
            WorkflowInstanceDTO dto = workflowService.startWorkflow(
                documentId, 
                templateId,
                null,      // title
                null,      // description
                "NORMAL"   // priority
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
            
            // ✅ UPDATED: Use the enhanced service method that returns detailed response
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
     * ✅ Create sample template (Admin only)
     * Maps to: POST /workflows/templates/create-sample
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/templates/create-sample")
    public ResponseEntity<WorkflowTemplate> createSampleTemplate() {
        try {
            WorkflowTemplate template = new WorkflowTemplate(
                    "Simple Document Approval",
                    "Manager then Admin approval",
                    WorkflowType.DOCUMENT_APPROVAL
            );
            template.setIsActive(true);
            template.setDefaultSlaHours(48);

            // Step 1: Manager approval
            WorkflowStep step1 = new WorkflowStep("Manager Approval", 1, StepType.APPROVAL);
            step1.setTemplate(template);
            step1.addRole(Role.MANAGER);

            // Step 2: Admin approval
            WorkflowStep step2 = new WorkflowStep("Admin Approval", 2, StepType.APPROVAL);
            step2.setTemplate(template);
            step2.addRole(Role.ADMIN);

            // Add steps to template
            template.getSteps().add(step1);
            template.getSteps().add(step2);

            WorkflowTemplate saved = templateRepository.save(template);
            log.info("Created sample template with ID: {}", saved.getId());
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to create sample template: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
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
            // ✅ UPDATED: Service now returns detailed task information
            List<Map<String, Object>> tasks = workflowService.getMyTasksWithDetails(getCurrentUserId());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Failed to get user tasks: {}", e.getMessage(), e);
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
            // ✅ FIXED: Service now returns List<WorkflowInstanceDTO>
            List<WorkflowInstanceDTO> workflows = workflowService.getMyWorkflows();
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            log.error("Failed to get user workflows: {}", e.getMessage(), e);
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
            // ✅ NEW: Enhanced service method for pagination
            Map<String, Object> response = workflowService.getUserWorkflowsWithDetails(
                getCurrentUserId(), page, size, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get paginated user workflows: {}", e.getMessage(), e);
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
            log.info("Completing task {} with action {}", taskId, action);
            
            // ✅ UPDATED: Use enhanced service method
            Map<String, Object> result = workflowService.processTaskActionWithUser(
                taskId, action.toString(), comments, getCurrentUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task completed successfully");
            response.put("taskId", taskId);
            response.put("action", action.toString());
            response.put("timestamp", LocalDateTime.now());
            response.putAll(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to complete task {}: {}", taskId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
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
            log.info("Getting workflow details for ID: {}", instanceId);
            
            // ✅ IMPLEMENTED: Use the enhanced service method
            WorkflowInstanceDTO workflow = workflowService.getWorkflowDetailsWithTasks(
                instanceId, getCurrentUserId());
            
            return ResponseEntity.ok(workflow);
            
        } catch (Exception e) {
            log.error("Failed to get workflow details for {}: {}", instanceId, e.getMessage(), e);
            throw e; // Let the global exception handler deal with it
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
            log.info("Cancelling workflow {} with reason: {}", instanceId, reason);
            
            // ✅ NEW: Use enhanced service method
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
            log.error("Failed to cancel workflow {}: {}", instanceId, e.getMessage(), e);
            
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
            // ✅ UPDATED: Service now returns WorkflowInstanceDTO
            WorkflowInstanceDTO workflow = workflowService.getWorkflowById(instanceId);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            log.error("Failed to get workflow {}: {}", instanceId, e.getMessage(), e);
            throw e;
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
            // This assumes you have a way to get user ID from username
            // You might need to inject UserService or UserRepository for this
            // For now, returning a placeholder - you'll need to implement this based on your user management
            return 1L; // TODO: Implement proper user ID retrieval
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage());
            throw new RuntimeException("Unable to get current user ID", e);
        }
    }
}
