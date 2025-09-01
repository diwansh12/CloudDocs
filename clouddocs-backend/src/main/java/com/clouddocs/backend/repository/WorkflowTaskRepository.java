package com.clouddocs.backend.repository;

import com.clouddocs.backend.dto.analytics.projections.StepMetricsProjection;
import com.clouddocs.backend.entity.TaskStatus;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowTask;
import com.clouddocs.backend.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    // ===== BASIC FINDER METHODS =====
    
    List<WorkflowTask> findByAssignedToAndStatus(User assignedTo, TaskStatus status);
    
    Page<WorkflowTask> findByAssignedTo(User assignedTo, Pageable pageable);
    
    Page<WorkflowTask> findByAssignedToAndStatus(User assignedTo, TaskStatus status, Pageable pageable);
    
    long countByStatus(TaskStatus status);
    
    long countByAssignedToAndStatus(User assignedTo, TaskStatus status);

    // ===== ENHANCED METHODS WITH JOIN FETCH =====

    /**
     * ✅ MISSING METHOD: Find tasks with workflow context
     */
    @Query("SELECT t FROM WorkflowTask t " +
           "LEFT JOIN FETCH t.workflowInstance wi " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.workflowStep " +
           "WHERE t.id = :id")
    Optional<WorkflowTask> findByIdWithWorkflow(@Param("id") Long id);

    /**
     * ✅ MISSING METHOD: Find tasks by assignee and status with proper ordering
     */
    @Query("SELECT t FROM WorkflowTask t " +
           "LEFT JOIN FETCH t.workflowInstance wi " +
           "LEFT JOIN FETCH t.workflowStep " +
           "WHERE t.assignedTo = :assignedTo AND t.status = :status " +
           "ORDER BY t.createdDate DESC")
    List<WorkflowTask> findByAssignedToAndStatusOrderByCreatedDateDesc(@Param("assignedTo") User assignedTo, @Param("status") TaskStatus status);

    /**
     * ✅ ALTERNATIVE METHOD: Same as above with different name
     */
    @Query("SELECT t FROM WorkflowTask t " +
           "LEFT JOIN FETCH t.workflowInstance wi " +
           "LEFT JOIN FETCH t.workflowStep " +
           "WHERE t.assignedTo = :assignedTo AND t.status = :status " +
           "ORDER BY t.createdDate DESC")
    List<WorkflowTask> findByAssignedToAndStatusOrderByCreatedAtDesc(@Param("assignedTo") User assignedTo, @Param("status") TaskStatus status);

    // ===== WORKFLOW INSTANCE ORDERING =====

    /**
     * ✅ CRITICAL FIX: Replace the problematic method with explicit @Query
     */
    @Query("SELECT t FROM WorkflowTask t " +
           "LEFT JOIN t.workflowStep ws " +
           "WHERE t.workflowInstance = :workflowInstance " +
           "ORDER BY ws.stepOrder ASC")
    List<WorkflowTask> findByWorkflowInstanceOrderByStepOrder(@Param("workflowInstance") WorkflowInstance workflowInstance);

    // SLA and Scheduler queries
    Page<WorkflowTask> findByStatusAndDueDateBefore(TaskStatus status, LocalDateTime dateTime, Pageable pageable);
    
    List<WorkflowTask> findByStatusAndDueDateBefore(TaskStatus status, LocalDateTime dateTime);
    
    @Query("SELECT t FROM WorkflowTask t WHERE t.status = :status AND t.dueDate < :cutoff ORDER BY t.dueDate ASC")
    Page<WorkflowTask> findOverdueTasksByStatus(@Param("status") TaskStatus status, 
                                                @Param("cutoff") LocalDateTime cutoff, 
                                                Pageable pageable);

    // Analytics queries
    @Query(value = """
        SELECT 
            ws.step_order AS stepOrder,
            AVG(CASE 
                  WHEN wt.completed_date IS NOT NULL AND wt.created_date IS NOT NULL
                  THEN EXTRACT(EPOCH FROM (wt.completed_date - wt.created_date)) / 3600.0
                  ELSE NULL
                END) AS avgTaskCompletionHours,
            SUM(CASE WHEN wt.action = 'APPROVE' THEN 1 ELSE 0 END) AS approvals,
            SUM(CASE WHEN wt.action = 'REJECT' THEN 1 ELSE 0 END) AS rejections,
            COUNT(wt.id) AS totalTasks,
            SUM(CASE WHEN wt.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedTasks,
            SUM(CASE WHEN wt.status = 'PENDING' THEN 1 ELSE 0 END) AS pendingTasks,
            SUM(CASE WHEN wt.status = 'OVERDUE' THEN 1 ELSE 0 END) AS overdueTasks
        FROM workflow_tasks wt
        JOIN workflow_steps ws ON ws.id = wt.workflow_step_id
        JOIN workflow_instances wi ON wi.id = wt.workflow_instance_id
        WHERE wi.start_date >= :from AND wi.start_date <= :to
        GROUP BY ws.step_order
        ORDER BY ws.step_order
        """, nativeQuery = true)
    List<StepMetricsProjection> aggregateByStepBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Tasks in date range
    @Query("SELECT t FROM WorkflowTask t WHERE t.createdDate >= :from AND t.createdDate <= :to")
    List<WorkflowTask> findTasksInDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Overdue tasks
    @Query("SELECT t FROM WorkflowTask t WHERE t.dueDate < :now AND t.status = 'PENDING'")
    List<WorkflowTask> findOverdueTasks(@Param("now") LocalDateTime now);

    // Tasks by workflow instance
    List<WorkflowTask> findByWorkflowInstanceIdOrderByCreatedDateAsc(Long workflowInstanceId);
    
    // Additional utility queries for scheduler
    @Query("SELECT COUNT(t) FROM WorkflowTask t WHERE t.status = :status AND t.dueDate < :cutoff")
    long countOverdueTasks(@Param("status") TaskStatus status, @Param("cutoff") LocalDateTime cutoff);
    
    @Query("SELECT t FROM WorkflowTask t WHERE t.status = :status AND t.assignedTo = :user ORDER BY t.dueDate ASC")
    List<WorkflowTask> findTasksByStatusAndAssignee(@Param("status") TaskStatus status, @Param("user") User user);
}
