package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.*;
import com.clouddocs.backend.security.AuthzUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ SIMPLIFIED: WorkflowService without retry complications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowStepRoleRepository stepRoleRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowHistoryRepository historyRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuthzUtil authz;
    private final NotificationService notificationService;

    @Autowired
    private AuditService auditService;
    


    private enum StepOutcome { CONTINUE, APPROVED, REJECTED }

    /**
     * ✅ MAIN METHOD: Start workflow - simplified without retry
     */
    @Transactional
    public WorkflowInstance startWorkflow(Long documentId, UUID templateId, 
                                    String title, String description, String priority) {
        log.info("🔄 Starting workflow - DocumentID: {}, TemplateID: {}, Priority: {}", 
                documentId, templateId, priority);

        try {
            // Load document
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

            // ✅ CRITICAL FIX: Use JOIN FETCH to load template with steps
          WorkflowTemplate template = templateRepository.findByIdWithStepsAndRoles(templateId)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow template not found"));


            if (!Boolean.TRUE.equals(template.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template is not active");
            }

            log.info("✅ Loaded template '{}' with {} steps", template.getName(), 
                    template.getSteps() != null ? template.getSteps().size() : 0);

            User initiator = getCurrentUser();

            // Create workflow instance
            WorkflowInstance instance = new WorkflowInstance(template, document, initiator, 
                                                            safeSlaHours(template.getDefaultSlaHours()));
            
            instance.setTitle(title != null ? title : "Workflow for " + document.getOriginalFilename());
            instance.setDescription(description);
            instance.setPriorityFromString(priority);
            instance.setStatus(WorkflowStatus.IN_PROGRESS);
            instance.setStartDate(LocalDateTime.now());
            instance.setCurrentStepOrder(1);

            // ✅ Save workflow instance
            instance = instanceRepository.saveAndFlush(instance);
            log.info("✅ Saved workflow instance with ID: {}", instance.getId());

            // Document becomes PENDING when workflow starts
            trySetDocumentPending(document);

            // ✅ SAFE: Generate tasks using the loaded template steps
            boolean tasksCreated = generateTasksForStepSafe(instance, template, 1);
            if (!tasksCreated) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No approvers found for step 1");
            }

            logHistory(instance, "WORKFLOW_STARTED", "Workflow started by " + safeName(initiator), initiator);
            
            auditService.logWorkflowAction(
                "Workflow Started: " + template.getName() + 
                (title != null && !title.isEmpty() ? " - " + title : ""),
                instance.getId().toString(),
                initiator.getUsername()
            );
            
            log.info("✅ Workflow instance {} created successfully", instance.getId());
            return instance;
            
        } catch (ResponseStatusException e) {
            // Re-throw HTTP exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("❌ Error starting workflow: {}", e.getMessage(), e);
            
            // ✅ Simple error handling without retry complications
            String errorMsg = "Failed to create workflow";
            if (e.getMessage() != null) {
                errorMsg += ": " + e.getMessage();
            }
            
            // Log failure for audit
            try {
                auditService.logWorkflowActionWithStatus(
                    "Workflow Creation Failed: " + e.getMessage(),
                    "Template-" + templateId,
                    getCurrentUser().getUsername(),
                    AuditLog.Status.FAILED
                );
            } catch (Exception auditEx) {
                log.warn("Failed to log audit: {}", auditEx.getMessage());
            }
            
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * ✅ SAFE: Task generation using pre-loaded template
     */
    private boolean generateTasksForStepSafe(WorkflowInstance instance, WorkflowTemplate template, int stepOrder) {
        try {
            log.info("🔄 Generating tasks for step {} using pre-loaded template", stepOrder);
            
            if (template.getSteps() == null || template.getSteps().isEmpty()) {
                log.warn("⚠️ Template has no steps defined");
                return false;
            }

            boolean anyTaskCreated = false;

            for (WorkflowStep step : template.getSteps()) {
                if (step.getStepOrder() == null || !step.getStepOrder().equals(stepOrder)) {
                    continue;
                }

                log.info("🔄 Processing step: {} (order: {})", step.getName(), step.getStepOrder());

                List<User> approvers = findApproversForStep(step);

                if (approvers.isEmpty()) {
                    log.warn("⚠️ No approvers found for step: {}", step.getName());
                    continue;
                }

                log.info("✅ Found {} approvers for step: {}", approvers.size(), step.getName());

                for (User user : approvers) {
                    WorkflowTask task = new WorkflowTask(instance, step, user, step.getName());
                    if (step.getSlaHours() != null && step.getSlaHours() > 0) {
                        task.setDueDate(LocalDateTime.now().plusHours(step.getSlaHours()));
                    } else if (instance.getDueDate() != null) {
                        task.setDueDate(instance.getDueDate());
                    }
                    taskRepository.save(task);

                    logHistory(instance, "TASK_ASSIGNED", "Task assigned to " + safeName(user), getCurrentUser());

                    try {
                        notificationService.notifyTaskAssigned(user, task);
                    } catch (Exception e) {
                        log.warn("Failed to send task assignment notification: {}", e.getMessage());
                    }

                    anyTaskCreated = true;
                }
            }

            log.info("✅ Task generation completed. Tasks created: {}", anyTaskCreated);
            return anyTaskCreated;
            
        } catch (Exception e) {
            log.error("❌ Error generating tasks for step {}: {}", stepOrder, e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ TASK COMPLETION: Simplified without retry
     */
    @Transactional
    public void completeTask(Long taskId, TaskAction action, String comments) {
        log.info("🔄 Attempting task completion - TaskID: {}, Action: {}", taskId, action);
        
        try {
            WorkflowTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found with ID: " + taskId));

            User current = getCurrentUser();

            // Authorization check
            boolean isAssignee = task.getAssignedTo() != null && task.getAssignedTo().getId().equals(current.getId());
            if (!isAssignee && !authz.isManagerOrAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Not authorized to complete this task. Task is assigned to: " + 
                    (task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : "unknown"));
            }

            WorkflowInstance instance = task.getWorkflowInstance();
            if (instance == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Task without workflow instance");
            }
            
            // Check workflow status
            if (instance.getStatus() == WorkflowStatus.APPROVED || 
                instance.getStatus() == WorkflowStatus.REJECTED || 
                instance.getStatus() == WorkflowStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Cannot complete task for a finalized workflow. Current status: " + instance.getStatus());
            }

            // Check task status
            if (task.getStatus() != TaskStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Task is not in PENDING status. Current status: " + task.getStatus());
            }

            // Complete the task
            task.complete(action, comments, current);
            taskRepository.saveAndFlush(task);

            logHistory(instance, "TASK_COMPLETED", 
                      "Task completed by " + safeName(current) + " with action " + action, current);

            String auditActivity = String.format("Task %s: %s%s", 
                action == TaskAction.APPROVE ? "Approved" : "Rejected",
                task.getTitle() != null ? task.getTitle() : "Workflow Task",
                comments != null && !comments.isEmpty() ? " - " + comments : ""
            );
            
            auditService.logWorkflowAction(auditActivity, instance.getId().toString(), current.getUsername());

            log.info("✅ Task {} completed by {} with action {}", taskId, current.getUsername(), action);

            // Notify task completion
            try {
                notificationService.notifyTaskCompleted(current, task, action);
            } catch (Exception e) {
                log.warn("Failed to send task completion notification: {}", e.getMessage());
            }

            // Evaluate step outcome
            WorkflowStep currentStep = getCurrentStepSafe(instance);
            if (currentStep != null) {
                StepOutcome outcome = evaluateStepOutcome(instance, currentStep);
                log.debug("Step outcome for step {}: {}", currentStep.getStepOrder(), outcome);
                handleStepOutcome(instance, currentStep, outcome);
            }
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error completing task {}: {}", taskId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to complete task: " + e.getMessage());
        }
    }

    /**
     * ✅ WORKFLOW CANCELLATION: Simplified without retry
     */
    @Transactional
    public void cancelWorkflow(Long instanceId, String reason) {
        log.info("🔄 Attempting to cancel workflow - InstanceID: {}, Reason: {}", instanceId, reason);
        
        try {
            WorkflowInstance instance = instanceRepository.findById(instanceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

            if (!authz.isInitiatorOrManager(instance)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to cancel workflow");
            }

            if (instance.getStatus() == WorkflowStatus.APPROVED || instance.getStatus() == WorkflowStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a finalized workflow");
            }
            
            if (instance.getStatus() == WorkflowStatus.CANCELLED) {
                return; // Idempotent cancel
            }

            WorkflowStatus oldStatus = instance.getStatus();
            instance.setStatus(WorkflowStatus.CANCELLED);
            instance.setEndDate(LocalDateTime.now());
            
            instance = instanceRepository.saveAndFlush(instance);
            
            log.info("✅ Workflow {} status updated: {} -> CANCELLED", instanceId, oldStatus);

            logHistory(instance, "WORKFLOW_CANCELLED", 
                      (reason == null || reason.isBlank()) ? "Cancelled" : reason, getCurrentUser());

            auditService.logWorkflowAction(
                "Workflow Cancelled: " + (reason != null && !reason.isBlank() ? reason : "No reason provided"),
                instance.getId().toString(),
                getCurrentUser().getUsername()
            );

            // Notify initiator
            try {
                if (instance.getInitiatedBy() != null) {
                    notificationService.notifyWorkflowRejected(instance.getInitiatedBy(), instance);
                }
            } catch (Exception e) {
                log.warn("Failed to send cancellation notification: {}", e.getMessage());
            }
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error cancelling workflow {}: {}", instanceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to cancel workflow: " + e.getMessage());
        }
    }

    /**
     * ✅ STRING-BASED INTERFACE: Process task action
     */
    public void processTaskAction(Long taskId, String action, String comments) {
        try {
            TaskAction taskAction = TaskAction.valueOf(action.toUpperCase());
            completeTask(taskId, taskAction, comments);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid action: " + action + ". Must be APPROVE or REJECT");
        }
    }

    // ===== QUERY METHODS =====
    
    @Transactional(readOnly = true)
    public List<WorkflowTask> getMyTasks() {
        try {
            return taskRepository.findByAssignedToAndStatus(getCurrentUser(), TaskStatus.PENDING);
        } catch (Exception e) {
            log.error("❌ Error getting user tasks: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowInstance> getMyWorkflows() {
        try {
            return instanceRepository.findByInitiatedByOrderByStartDateDesc(getCurrentUser());
        } catch (Exception e) {
            log.error("❌ Error getting user workflows: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public WorkflowInstance getWorkflowById(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found with ID: " + instanceId));
    }

    @Transactional(readOnly = true)
    public WorkflowTask getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found with ID: " + taskId));
    }

    // ===== ALL HELPER METHODS REMAIN THE SAME =====
    
    private WorkflowStep getCurrentStepSafe(WorkflowInstance instance) {
        try {
            if (instance.getCurrentStepOrder() == null) return null;
            
            List<WorkflowTask> tasks = taskRepository.findByWorkflowInstanceOrderByStepOrder(instance);
            for (WorkflowTask task : tasks) {
                if (task.getWorkflowStep() != null && 
                    task.getWorkflowStep().getStepOrder() != null &&
                    task.getWorkflowStep().getStepOrder().equals(instance.getCurrentStepOrder())) {
                    return task.getWorkflowStep();
                }
            }
            
            List<WorkflowStep> steps = stepRepository.findByTemplateOrderByStepOrderAsc(instance.getTemplate());
            return steps.stream()
                    .filter(s -> s.getStepOrder() != null && 
                               s.getStepOrder().equals(instance.getCurrentStepOrder()))
                    .findFirst()
                    .orElse(null);
                    
        } catch (Exception e) {
            log.error("❌ Error getting current step for workflow {}: {}", instance.getId(), e.getMessage());
            return null;
        }
    }

    private StepOutcome evaluateStepOutcome(WorkflowInstance instance, WorkflowStep step) {
        List<WorkflowTask> stepTasks = getStepTasks(instance, step);
        
        long approvals = stepTasks.stream()
                .filter(t -> t.getAction() == TaskAction.APPROVE).count();
        long rejections = stepTasks.stream()
                .filter(t -> t.getAction() == TaskAction.REJECT).count();
        long totalAssigned = stepTasks.size();
        
        ApprovalPolicy policy = step.getApprovalPolicy() != null ? 
                               step.getApprovalPolicy() : ApprovalPolicy.QUORUM;
        
        switch (policy) {
            case UNANIMOUS:
                if (rejections > 0) return StepOutcome.REJECTED;
                if (approvals == totalAssigned) return StepOutcome.APPROVED;
                break;
            case MAJORITY:
                long needed = (totalAssigned / 2) + 1;
                if (approvals >= needed) return StepOutcome.APPROVED;
                if (rejections >= needed) return StepOutcome.REJECTED;
                break;
            case ANY_ONE:
                if (approvals > 0) return StepOutcome.APPROVED;
                if (rejections > 0) return StepOutcome.REJECTED;
                break;
            case QUORUM:
            default:
                int required = (step.getRequiredApprovals() == null || step.getRequiredApprovals() <= 0) 
                              ? 1 : step.getRequiredApprovals();
                if (approvals >= required) return StepOutcome.APPROVED;
                if (rejections > 0) return StepOutcome.REJECTED;
                break;
        }
        
        return StepOutcome.CONTINUE;
    }

    private List<WorkflowTask> getStepTasks(WorkflowInstance instance, WorkflowStep step) {
        return instance.getTasks().stream()
                .filter(t -> sameStep(t, step))
                .collect(Collectors.toList());
    }

    private boolean sameStep(WorkflowTask t, WorkflowStep step) {
        return t.getWorkflowStep() != null && 
               t.getWorkflowStep().getId().equals(step.getId());
    }

    private void handleStepOutcome(WorkflowInstance instance, WorkflowStep step, StepOutcome outcome) {
        switch (outcome) {
            case REJECTED:
                cancelRemainingTasks(instance, step, "Step rejected");
                progressToNextStep(instance, TaskAction.REJECT);
                break;
            case APPROVED:
                cancelRemainingTasks(instance, step, "Quorum reached - step approved");
                progressToNextStep(instance, TaskAction.APPROVE);
                break;
            case CONTINUE:
                log.debug("Step {} still waiting for more approvals", step.getStepOrder());
                break;
        }
    }

    private void cancelRemainingTasks(WorkflowInstance instance, WorkflowStep step, String reason) {
        List<WorkflowTask> pendingTasks = instance.getTasks().stream()
                .filter(t -> sameStep(t, step) && t.getStatus() == TaskStatus.PENDING)
                .collect(Collectors.toList());
        
        User systemUser = getCurrentUser();
        
        for (WorkflowTask task : pendingTasks) {
            try {
                task.setStatus(TaskStatus.COMPLETED);
                task.setAction(TaskAction.REJECT);
                task.setComments(reason);
                task.setCompletedBy(systemUser);
                task.setCompletedDate(LocalDateTime.now());
                
                taskRepository.saveAndFlush(task);
                logHistory(instance, "TASK_CANCELLED", 
                          "Task auto-cancelled: " + reason + " for " + safeName(task.getAssignedTo()), 
                          systemUser);
            } catch (Exception e) {
                log.warn("Failed to cancel task {}: {}", task.getId(), e.getMessage());
            }
        }
    }

    private void progressToNextStep(WorkflowInstance instance, TaskAction lastAction) {
        log.info("🔄 Progressing workflow {} to next step, last action: {}", instance.getId(), lastAction);
        
        if (lastAction == TaskAction.REJECT) {
            instance.setStatus(WorkflowStatus.REJECTED);
            instance.setEndDate(LocalDateTime.now());
            instanceRepository.saveAndFlush(instance);

            tryUpdateDocumentOnRejection(instance);
            logHistory(instance, "WORKFLOW_REJECTED", "Workflow rejected", getCurrentUser());

            try {
                if (instance.getInitiatedBy() != null) {
                    notificationService.notifyWorkflowRejected(instance.getInitiatedBy(), instance);
                }
            } catch (Exception e) {
                log.warn("Failed to send rejection notification: {}", e.getMessage());
            }
            return;
        }

        int totalSteps = stepRepository.findByTemplateOrderByStepOrderAsc(instance.getTemplate()).size();
        if (instance.getCurrentStepOrder() == null) {
            instance.setCurrentStepOrder(1);
        }

        if (instance.getCurrentStepOrder() >= totalSteps) {
            instance.setStatus(WorkflowStatus.APPROVED);
            instance.setEndDate(LocalDateTime.now());
            instanceRepository.saveAndFlush(instance);

            tryUpdateDocumentOnApproval(instance);
            logHistory(instance, "WORKFLOW_APPROVED", "Workflow approved", getCurrentUser());

            try {
                if (instance.getInitiatedBy() != null) {
                    notificationService.notifyWorkflowApproved(instance.getInitiatedBy(), instance);
                }
            } catch (Exception e) {
                log.warn("Failed to send approval notification: {}", e.getMessage());
            }
        } else {
            instance.setCurrentStepOrder(instance.getCurrentStepOrder() + 1);
            instanceRepository.saveAndFlush(instance);

            boolean created = generateTasksForStep(instance, instance.getCurrentStepOrder());
            if (!created) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No approvers found for step " + instance.getCurrentStepOrder());
            }
            logHistory(instance, "STEP_STARTED", "Step " + instance.getCurrentStepOrder() + " started", getCurrentUser());
        }
    }

    // ===== ALL OTHER HELPER METHODS REMAIN THE SAME =====
    
    private boolean generateTasksForStep(WorkflowInstance instance, int stepOrder) {
        try {
            List<WorkflowStep> steps = stepRepository.findByTemplateOrderByStepOrderAsc(instance.getTemplate());
            if (steps == null || steps.isEmpty()) return false;

            boolean anyTaskCreated = false;

            for (WorkflowStep step : steps) {
                if (step.getStepOrder() == null || step.getStepOrder() != stepOrder) continue;

                List<User> approvers = findApproversForStep(step);
                if (approvers.isEmpty()) continue;

                for (User user : approvers) {
                    WorkflowTask task = new WorkflowTask(instance, step, user, step.getName());
                    if (step.getSlaHours() != null && step.getSlaHours() > 0) {
                        task.setDueDate(LocalDateTime.now().plusHours(step.getSlaHours()));
                    } else if (instance.getDueDate() != null) {
                        task.setDueDate(instance.getDueDate());
                    }
                    taskRepository.save(task);

                    logHistory(instance, "TASK_ASSIGNED", "Task assigned to " + safeName(user), getCurrentUser());

                    try {
                        notificationService.notifyTaskAssigned(user, task);
                    } catch (Exception e) {
                        log.warn("Failed to send task assignment notification: {}", e.getMessage());
                    }

                    anyTaskCreated = true;
                }
            }

            return anyTaskCreated;
        } catch (Exception e) {
            log.error("❌ Error generating tasks for step {}: {}", stepOrder, e.getMessage(), e);
            return false;
        }
    }

    private List<User> findApproversForStep(WorkflowStep step) {
        List<User> approvers = new ArrayList<>();

        if (step.getAssignedApprovers() != null && !step.getAssignedApprovers().isEmpty()) {
            approvers.addAll(step.getAssignedApprovers());
            return approvers;
        }

        List<WorkflowStepRole> stepRoles = stepRoleRepository.findByStepId(step.getId());

        if (!stepRoles.isEmpty()) {
            List<Role> requiredRoles = stepRoles.stream()
                    .map(WorkflowStepRole::getRoleName)
                    .distinct()
                    .collect(Collectors.toList());

            for (Role role : requiredRoles) {
                List<User> usersWithRole = userRepository.findByRole(role);
                if (usersWithRole != null && !usersWithRole.isEmpty()) {
                    for (User user : usersWithRole) {
                        if (!approvers.contains(user)) {
                            approvers.add(user);
                        }
                    }
                }
            }
        }

        return approvers;
    }

    private void logHistory(WorkflowInstance instance, String action, String details, User performedBy) {
        try {
            WorkflowHistory history = new WorkflowHistory(instance, action, details, performedBy);
            historyRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to log workflow history: {}", e.getMessage());
        }
    }

    private User getCurrentUser() {
        String username = null;
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {}
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged in user not found"));
    }

    private Integer safeSlaHours(Integer hours) {
        if (hours == null || hours <= 0) return null;
        return hours;
    }

    private String safeName(User user) {
        if (user == null) return "Unknown";
        String n = user.getFullName();
        return (n == null || n.isBlank()) ? user.getUsername() : n;
    }

    private void trySetDocumentPending(Document document) {
        if (document == null) return;
        try {
            if (document.getStatus() == null || document.getStatus() == DocumentStatus.DRAFT) {
                document.setStatus(DocumentStatus.PENDING);
                document.setApprovedBy(null);
                document.setApprovalDate(null);
                document.setRejectionReason(null);
                documentRepository.save(document);
            }
        } catch (Exception e) {
            log.warn("Failed to set document pending: {}", e.getMessage());
        }
    }

    private void tryUpdateDocumentOnApproval(WorkflowInstance instance) {
        try {
            Document doc = instance.getDocument();
            if (doc == null) return;

            doc.setStatus(DocumentStatus.APPROVED);

            WorkflowTask lastApprovalTask = null;
            if (instance.getTasks() != null) {
                lastApprovalTask = instance.getTasks().stream()
                        .filter(t -> t.getCompletedDate() != null && t.getAction() == TaskAction.APPROVE)
                        .max(Comparator.comparing(WorkflowTask::getCompletedDate))
                        .orElse(null);
            }

            if (lastApprovalTask != null) {
                doc.setApprovedBy(lastApprovalTask.getCompletedBy());
                doc.setApprovalDate(instance.getEndDate() != null ? instance.getEndDate() : LocalDateTime.now());
            } else {
                doc.setApprovedBy(instance.getInitiatedBy());
                doc.setApprovalDate(instance.getEndDate() != null ? instance.getEndDate() : LocalDateTime.now());
            }

            doc.setRejectionReason(null);
            documentRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to update document on approval: {}", e.getMessage());
        }
    }

    private void tryUpdateDocumentOnRejection(WorkflowInstance instance) {
        try {
            Document doc = instance.getDocument();
            if (doc == null) return;

            doc.setStatus(DocumentStatus.REJECTED);

            WorkflowTask lastTask = null;
            if (instance.getTasks() != null) {
                lastTask = instance.getTasks().stream()
                        .filter(t -> t.getCompletedDate() != null)
                        .max(Comparator.comparing(WorkflowTask::getCompletedDate))
                        .orElse(null);
            }

            if (lastTask != null) {
                doc.setRejectionReason(lastTask.getComments());
            }

            doc.setApprovedBy(null);
            doc.setApprovalDate(null);

            documentRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to update document on rejection: {}", e.getMessage());
        }
    }
}
