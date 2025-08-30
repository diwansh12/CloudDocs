package com.clouddocs.backend.repository;

import com.clouddocs.backend.dto.analytics.projections.StepMetricsProjection;
import com.clouddocs.backend.entity.TaskStatus;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    // Basic queries
    List<WorkflowTask> findByAssignedToAndStatus(User assignedTo, TaskStatus status);
    
    Page<WorkflowTask> findByAssignedTo(User assignedTo, Pageable pageable);
    
    Page<WorkflowTask> findByAssignedToAndStatus(User assignedTo, TaskStatus status, Pageable pageable);
    
    long countByStatus(TaskStatus status);
    
    long countByAssignedToAndStatus(User assignedTo, TaskStatus status);

    // SLA and Scheduler queries - THESE WERE MISSING
    Page<WorkflowTask> findByStatusAndDueDateBefore(TaskStatus status, LocalDateTime dateTime, Pageable pageable);
    
    List<WorkflowTask> findByStatusAndDueDateBefore(TaskStatus status, LocalDateTime dateTime);
    
    // More specific overdue queries
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
