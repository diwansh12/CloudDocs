package com.clouddocs.backend.scheduler;

import com.clouddocs.backend.entity.*;
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

import java.time.LocalDateTime;
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

        LocalDateTime now = LocalDateTime.now();
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
        LocalDateTime escalationCutoff = now.minusHours(escalationGraceHours);
        var escalatePage = taskRepository.findByStatusAndDueDateBefore(TaskStatus.OVERDUE, escalationCutoff, batch);
        List<WorkflowTask> toEscalate = escalatePage.getContent();

        if (!toEscalate.isEmpty()) {
            logger.info("Escalating {} overdue tasks (cutoff: {})", toEscalate.size(), escalationCutoff);
        }

        Role escalationRole = safeRole(escalationRoleName);

        for (WorkflowTask task : toEscalate) {
            try {
                // Reassign to a user with escalation role (pick first by simple rule; customize as needed)
                List<User> candidates = userRepository.findByRole(escalationRole);
                if (candidates == null || candidates.isEmpty()) {
                    logger.warn("No users found for escalation role {}", escalationRole);
                    continue;
                }

                User newAssignee = pickEscalationAssignee(candidates);

                User previousAssignee = task.getAssignedTo();
                task.setAssignedTo(newAssignee);
                // Optionally bump due date (e.g., +24h from now)
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

    private Role safeRole(String roleName) {
        try {
            return Role.valueOf(roleName.toUpperCase());
        } catch (Exception e) {
            return Role.MANAGER;
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
