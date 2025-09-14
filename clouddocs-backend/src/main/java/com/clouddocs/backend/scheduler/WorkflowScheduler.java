package com.clouddocs.backend.scheduler;

import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.RoleRepository;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.WorkflowHistoryRepository;
import com.clouddocs.backend.repository.WorkflowTaskRepository;
import com.clouddocs.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Scheduled tasks for SLA handling: overdue marking and escalations.
 */
@Component
public class WorkflowScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowScheduler.class);

    @Autowired private WorkflowTaskRepository taskRepository;
    @Autowired private WorkflowHistoryRepository historyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository; // ✅ ADDED: For Role entity access
    @Autowired private NotificationService notificationService;

    @Value("${workflow.sla.enabled:true}")
    private boolean slaEnabled;

    @Value("${workflow.escalation.enabled:true}")
    private boolean escalationEnabled;

    @Value("${workflow.escalation.graceHours:24}")
    private long escalationGraceHours;

    @Value("${workflow.escalation.role:MANAGER}")
    private String escalationRoleName;

    // Run every 10 minutes with 2 minutes initial delay
    @Scheduled(fixedDelay = 600_000L, initialDelay = 120_000L)
    @Transactional
    public void processOverdueAndEscalations() {
        if (!slaEnabled) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Pageable batch = PageRequest.of(0, 200);

        // 1) Mark tasks PENDING and dueDate < now as OVERDUE
        var overduePage = taskRepository.findByStatusAndDueDateBefore(TaskStatus.PENDING, now, batch);
        List<WorkflowTask> toOverdue = overduePage.getContent();

        if (!toOverdue.isEmpty()) {
            logger.info("Marking {} tasks as OVERDUE", toOverdue.size());
        }

        for (WorkflowTask task : toOverdue) {
            try {
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);

                // Notify assignee
                if (task.getAssignedTo() != null) {
                    notificationService.notifyTaskOverdue(task.getAssignedTo(), task);
                }

                // Log history
                addHistory(task, "TASK_OVERDUE", "Task marked as overdue");
            } catch (Exception e) {
                logger.error("Failed to mark task {} as OVERDUE", task.getId(), e);
            }
        }

        if (!escalationEnabled) {
            return;
        }

        // 2) Escalate tasks OVERDUE older than graceHours
        OffsetDateTime escalationCutoff = now.minusHours(escalationGraceHours);
        var escalatePage = taskRepository.findByStatusAndDueDateBefore(TaskStatus.OVERDUE, escalationCutoff, batch);
        List<WorkflowTask> toEscalate = escalatePage.getContent();

        if (!toEscalate.isEmpty()) {
            logger.info("Escalating {} overdue tasks (cutoff: {})", toEscalate.size(), escalationCutoff);
        }

        // ✅ FIXED: Get Role entity instead of enum
        Role escalationRole = safeRole(escalationRoleName);

        for (WorkflowTask task : toEscalate) {
            try {
                // ✅ FIXED: Find users by Role entity using Many-to-Many query
                List<User> candidates = userRepository.findByRoleName(ERole.valueOf("ROLE_" + escalationRoleName.toUpperCase()));
                if (candidates == null || candidates.isEmpty()) {
                    logger.warn("No users found for escalation role {}", escalationRoleName);
                    continue;
                }

                User newAssignee = pickEscalationAssignee(candidates);

                User previousAssignee = task.getAssignedTo();
                task.setAssignedTo(newAssignee);
                
                task.setDueDate(now.plusHours(24));
                taskRepository.save(task);

                // Log escalation
                String details = String.format("Task escalated from %s to %s",
                        safeUser(previousAssignee), safeUser(newAssignee));
                addHistory(task, "TASK_ESCALATED", details);

                // Notify new assignee and optionally previous
                notificationService.notifyTaskAssigned(newAssignee, task);
                if (previousAssignee != null) {
                    notificationService.notifyTaskOverdue(previousAssignee, task);
                }

            } catch (Exception e) {
                logger.error("Failed to escalate task {}", task.getId(), e);
            }
        }
    }

    // ✅ COMPLETELY FIXED: Get Role entity from database
    private Role safeRole(String roleName) {
        try {
            // Convert string to ERole enum (e.g., "MANAGER" -> "ROLE_MANAGER")
            String enumName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            ERole eRole = ERole.valueOf(enumName.toUpperCase());
            
            // Find Role entity by ERole enum
            return roleRepository.findByName(eRole)
                .orElseGet(() -> {
                    logger.warn("Role {} not found, falling back to ROLE_MANAGER", enumName);
                    return roleRepository.findByName(ERole.ROLE_MANAGER)
                        .orElse(null);
                });
        } catch (Exception e) {
            logger.warn("Error resolving role {}, falling back to ROLE_MANAGER: {}", roleName, e.getMessage());
            // ✅ FIXED: Return ROLE_MANAGER entity as fallback
            return roleRepository.findByName(ERole.ROLE_MANAGER)
                .orElse(null);
        }
    }

    private User pickEscalationAssignee(List<User> candidates) {
        // Simple strategy: first candidate; replace with round-robin or least-loaded if needed
        return candidates.get(0);
    }

    private void addHistory(WorkflowTask task, String action, String details) {
        try {
            WorkflowInstance instance = task.getWorkflowInstance();
            if (instance == null) return;
            WorkflowHistory history = new WorkflowHistory(instance, action, details, null);
            historyRepository.save(history);
        } catch (Exception e) {
            logger.warn("Failed to add history for escalated/overdue task {}", task.getId(), e);
        }
    }

    private String safeUser(User u) {
        if (u == null) return "Unassigned";
        return (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getUsername();
    }
}
