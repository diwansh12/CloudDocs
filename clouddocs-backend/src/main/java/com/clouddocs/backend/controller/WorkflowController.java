package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.CreateWorkflowRequest;
import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.mapper.WorkflowMapper;
import com.clouddocs.backend.repository.WorkflowTemplateRepository;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.UserPrincipal;
import com.clouddocs.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/workflows")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowTemplateRepository templateRepository;
    private final UserRepository userRepository;

    /**
     * ✅ ENHANCED: Create workflow with proper user assignment
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        try {
            log.info("🚀 Creating workflow with request: {}", request);
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                throw new IllegalStateException("No authenticated user found");
            }
            
            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
            log.debug("Authenticated user: {} (ID: {})", userPrincipal.getUsername(), userPrincipal.getId());
            
            // Validate required fields
            if (request.getDocumentId() == null) {
                throw new IllegalArgumentException("Document ID is required");
            }
            if (request.getTemplateId() == null) {
                throw new IllegalArgumentException("Template ID is required");
            }
            
            // ✅ FIXED: Create workflow with proper user context
            WorkflowInstance instance = workflowService.startWorkflowWithUser(
                request.getDocumentId(),
                request.getTemplateId(),
                request.getTitle() != null ? request.getTitle() : "Document Approval Workflow",
                request.getDescription(),
                request.getPriority() != null ? request.getPriority() : "NORMAL",
                userPrincipal.getId() // ✅ Pass user ID explicitly
            );
            
            WorkflowInstanceDTO dto = WorkflowMapper.toInstanceDTO(instance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow created successfully");
            response.put("workflow", dto);
            response.put("workflowId", instance.getId());
            response.put("assignedTo", instance.getInitiatedBy() != null ? 
                instance.getInitiatedBy().getFullName() : "System");
            response.put("timestamp", LocalDateTime.now());
            
            log.info("✅ Workflow created successfully with ID: {}, Assigned to: {}", 
                    instance.getId(), 
                    instance.getInitiatedBy() != null ? instance.getInitiatedBy().getFullName() : "System");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error creating workflow: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to create workflow: " + e.getMessage(),
                "type", e.getClass().getSimpleName(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * ✅ ENHANCED: Get user workflows with safe DTO conversion
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getUserWorkflows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        try {
            log.info("🔍 Getting workflows for current user - page: {}, size: {}, status: {}", 
                    page, size, status);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            Long userId = userPrincipal.getId();
            
            // Get workflows using service method
            Map<String, Object> workflows = workflowService.getUserWorkflowsWithDetails(userId, page, size, status);
            
            log.info("✅ Retrieved {} workflows for user {}", 
                    ((List<?>) workflows.get("workflows")).size(), userPrincipal.getUsername());
            
            return ResponseEntity.ok(workflows);
            
        } catch (Exception e) {
            log.error("❌ Error getting user workflows: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "workflows", new ArrayList<>(),
                "totalItems", 0,
                "totalPages", 0,
                "currentPage", page,
                "pageSize", size,
                "hasNext", false,
                "hasPrevious", false
            ));
        }
    }

    /**
     * ✅ ENHANCED: Handle task actions with proper validation
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/tasks/{taskId}/action")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleTaskAction(
            @PathVariable Long taskId,
            @RequestParam String action,
            @RequestParam(required = false) String comments) {
        
        try {
            log.info("🔄 Processing task action - TaskID: {}, Action: {}", taskId, action);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            // Validate action parameter
            if (!action.equalsIgnoreCase("APPROVE") && !action.equalsIgnoreCase("REJECT")) {
                throw new IllegalArgumentException("Invalid action. Must be APPROVE or REJECT");
            }
            
            // Process the action using service method with user context
            Map<String, Object> result = workflowService.processTaskActionWithUser(
                taskId, action, comments, userPrincipal.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task " + action.toLowerCase() + "d successfully");
            response.put("taskId", taskId);
            response.put("action", action);
            response.put("workflowStatus", result.get("workflowStatus"));
            response.put("nextStep", result.get("nextStep"));
            response.put("timestamp", LocalDateTime.now());
            
            log.info("✅ Task action completed successfully - TaskID: {}, Action: {}", taskId, action);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request for task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "taskId", taskId
            ));
            
        } catch (Exception e) {
            log.error("❌ Unexpected error processing task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to process task action: " + e.getMessage(),
                "taskId", taskId,
                "action", action
            ));
        }
    }

    /**
     * ✅ ENHANCED: Get workflow details with tasks and steps
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{workflowId}/details")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getWorkflowDetails(@PathVariable Long workflowId) {
        try {
            log.info("🔍 Getting workflow details for ID: {}", workflowId);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            Map<String, Object> details = workflowService.getWorkflowDetailsWithTasks(workflowId, userPrincipal.getId());
            
            log.info("✅ Workflow details retrieved for ID: {}", workflowId);
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            log.error("❌ Error getting workflow details for ID {}: {}", workflowId, e.getMessage(), e);
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Workflow not found or access denied",
                "workflowId", workflowId
            ));
        }
    }

    /**
     * ✅ ENHANCED: Get tasks assigned to current user
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tasks/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getMyTasks() {
        try {
            log.info("🔍 Getting tasks for current user");
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            List<Map<String, Object>> tasks = workflowService.getMyTasksWithDetails(userPrincipal.getId());
            
            log.info("✅ Retrieved {} tasks for user {}", tasks.size(), userPrincipal.getUsername());
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            log.error("❌ Error getting user tasks: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * ✅ ENHANCED: Legacy endpoint compatibility
     */
    @PostMapping("/start")
    @Transactional
    public ResponseEntity<Map<String, Object>> startWorkflow(
            @RequestParam Long documentId,
            @RequestParam UUID templateId) {
        
        try {
            log.info("🚀 Starting workflow (legacy endpoint) for document {} with template {}", 
                    documentId, templateId);
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            
            WorkflowInstance instance = workflowService.startWorkflowWithUser(
                documentId, 
                templateId,
                "Document Approval Workflow", // Default title
                null,      // description
                "NORMAL",   // priority
                userPrincipal.getId()
            );
            
            WorkflowInstanceDTO dto = WorkflowMapper.toInstanceDTO(instance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("workflow", dto);
            response.put("workflowId", instance.getId());
            response.put("message", "Workflow created successfully");
            
            log.info("✅ Legacy workflow created successfully with ID: {}", instance.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error creating workflow for document {} with template {}: {}", 
                     documentId, templateId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName(),
                "timestamp", LocalDateTime.now(),
                "documentId", documentId,
                "templateId", templateId.toString()
            ));
        }
    }

    /**
     * ✅ ENHANCED: Create sample template for testing
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/templates/create-sample")
    @Transactional
    public ResponseEntity<Map<String, Object>> createSampleTemplate() {
        try {
            log.info("🔧 Creating sample workflow template");
            
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            User creator = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Creator not found"));
            
            WorkflowTemplate template = new WorkflowTemplate(
                    "Sample Document Approval",
                    "Manager then Admin approval workflow",
                    WorkflowType.DOCUMENT_APPROVAL
            );
            template.setIsActive(true);
            template.setDefaultSlaHours(48);
            template.setCreatedBy(creator);
            template.setCreatedAt(LocalDateTime.now());

            // Step 1: Manager approval
            WorkflowStep step1 = new WorkflowStep("Manager Review", 1, StepType.APPROVAL);
            step1.setTemplate(template);
            step1.setAssigneeRole(Role.MANAGER);
            step1.setIsRequired(true);
            step1.setSlaHours(24);

            // Step 2: Admin approval
            WorkflowStep step2 = new WorkflowStep("Admin Final Approval", 2, StepType.APPROVAL);
            step2.setTemplate(template);
            step2.setAssigneeRole(Role.ADMIN);
            step2.setIsRequired(true);
            step2.setSlaHours(24);

            // Add steps to template
            template.getSteps().add(step1);
            template.getSteps().add(step2);

            WorkflowTemplate saved = templateRepository.save(template);
            
            log.info("✅ Created sample template with ID: {}", saved.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "template", saved,
                "templateId", saved.getId(),
                "message", "Sample template created successfully"
            ));
            
        } catch (Exception e) {
            log.error("❌ Failed to create sample template: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to create sample template: " + e.getMessage()
            ));
        }
    }

    // ===== HELPER METHODS =====

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new RuntimeException("No authenticated user found");
        }
        return (UserPrincipal) auth.getPrincipal();
    }
}
