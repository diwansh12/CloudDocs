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
            
            // ✅ FIXED: Call service with all parameters
            WorkflowInstance instance = workflowService.startWorkflow(
                request.getDocumentId(),
                request.getTemplateId(),
                request.getTitle(),           // ✅ Include title
                request.getDescription(),     // ✅ Include description  
                request.getPriority()         // ✅ Include priority
            );
            
            WorkflowInstanceDTO dto = WorkflowMapper.toInstanceDTO(instance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow created successfully");
            response.put("workflow", dto);
            response.put("workflowId", instance.getId());
            response.put("timestamp", LocalDateTime.now());
            
            log.info("✅ Workflow created successfully with ID: {}, Priority: {}", 
                    instance.getId(), request.getPriority());
            
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
            
            // ✅ FIXED: Call the 5-parameter method with defaults
            WorkflowInstance instance = workflowService.startWorkflow(
                documentId, 
                templateId,
                null,      // title
                null,      // description
                "NORMAL"   // priority
            );
            
            WorkflowInstanceDTO dto = WorkflowMapper.toInstanceDTO(instance);
            log.info("Workflow created successfully with ID: {}", instance.getId());
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
     * ✅ Handle task actions (approve/reject)
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
            
            // Process the action using service method
            workflowService.processTaskAction(taskId, action, comments);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task " + action.toLowerCase() + "d successfully");
            response.put("taskId", taskId);
            response.put("action", action);
            response.put("timestamp", LocalDateTime.now());
            
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
     * ✅ Get tasks assigned to current user
     * Maps to: GET /workflows/tasks/user
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tasks/user")
    public ResponseEntity<List<WorkflowTask>> getUserTasks() {
        try {
            List<WorkflowTask> tasks = workflowService.getMyTasks();
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
    public ResponseEntity<List<WorkflowInstance>> getUserWorkflows() {
        try {
            List<WorkflowInstance> workflows = workflowService.getMyWorkflows();
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            log.error("Failed to get user workflows: {}", e.getMessage(), e);
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
            
            workflowService.completeTask(taskId, action, comments);
            
            return ResponseEntity.ok(Map.of(
                "message", "Task completed successfully",
                "taskId", taskId,
                "action", action.toString(),
                "timestamp", LocalDateTime.now()
            ));
            
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
     * ✅ Get workflow details by ID (put after specific paths)
     * Maps to: GET /workflows/{instanceId}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{instanceId}")
    public ResponseEntity<Map<String, Object>> getWorkflowDetails(@PathVariable Long instanceId) {
        try {
            log.info("Getting workflow details for ID: {}", instanceId);
            
            // TODO: Implement this method in WorkflowService
            // For now, return a meaningful response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workflow details endpoint - implementation pending");
            response.put("instanceId", instanceId);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get workflow details for {}: {}", instanceId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "instanceId", instanceId,
                "timestamp", LocalDateTime.now()
            ));
        }
    }
}

