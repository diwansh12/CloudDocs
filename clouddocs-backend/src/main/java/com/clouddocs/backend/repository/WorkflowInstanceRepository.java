package com.clouddocs.backend.repository;

import com.clouddocs.backend.dto.analytics.projections.StatusCountProjection;
import com.clouddocs.backend.dto.analytics.projections.TemplateCountProjection;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowInstance;
import com.clouddocs.backend.entity.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    // ===== BASIC FINDER METHODS (Paginated) =====
    
    Page<WorkflowInstance> findByInitiatedBy(User user, Pageable pageable);
    Page<WorkflowInstance> findByStatus(WorkflowStatus status, Pageable pageable);
    Page<WorkflowInstance> findByTemplateId(UUID templateId, Pageable pageable);
    Page<WorkflowInstance> findByInitiatedByAndStatus(User user, WorkflowStatus status, Pageable pageable);
    Page<WorkflowInstance> findByInitiatedByAndStartDateBetween(User user, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<WorkflowInstance> findByInitiatedByAndStatusAndTemplateId(User user, WorkflowStatus status, UUID templateId, Pageable pageable);

    // ===== BASIC FINDER METHODS (List - Non-paginated) =====
    
    List<WorkflowInstance> findByInitiatedBy(User user);
    List<WorkflowInstance> findByStatus(WorkflowStatus status);
    List<WorkflowInstance> findByInitiatedByOrderByStartDateDesc(User initiatedBy);
    List<WorkflowInstance> findByStatusOrderByStartDateDesc(WorkflowStatus status);
    List<WorkflowInstance> findByTemplateIdOrderByStartDateDesc(UUID templateId);

    // ===== ENHANCED METHODS WITH JOIN FETCH (Critical for preventing lazy loading) =====
    
    /**
     * ✅ MAIN METHOD: Enhanced query for /mine endpoint - prevents LazyInitializationException
     */
    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByInitiatedByWithDetails(@Param("user") User user, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user AND w.status = :status " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByInitiatedByAndStatusWithDetails(@Param("user") User user, @Param("status") WorkflowStatus status, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user AND tpl.id = :templateId " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByInitiatedByAndTemplateIdWithDetails(@Param("user") User user, @Param("templateId") UUID templateId, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user AND w.status = :status AND tpl.id = :templateId " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByInitiatedByAndStatusAndTemplateIdWithDetails(@Param("user") User user, @Param("status") WorkflowStatus status, @Param("templateId") UUID templateId, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user AND w.startDate BETWEEN :from AND :to " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByInitiatedByAndStartDateBetweenWithDetails(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE w.status = :status " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByStatusWithDetails(@Param("status") WorkflowStatus status, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE tpl.id = :templateId " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findByTemplateIdWithDetails(@Param("templateId") UUID templateId, Pageable pageable);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> findAllWithDetails(Pageable pageable);

    // ===== INDIVIDUAL ENTITY FETCH METHODS =====
    
    @Query("SELECT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document " +
           "LEFT JOIN FETCH w.initiatedBy " +
           "LEFT JOIN FETCH w.template " +
           "WHERE w.id = :id")
    Optional<WorkflowInstance> findByIdWithBasicDetails(@Param("id") Long id);

    /**
     * ✅ FIXED: Corrected query using 'workflowStep' instead of 'step'
     */
    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks t " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.workflowStep " +
           "WHERE w.id = :id")
    Optional<WorkflowInstance> findByIdWithTasksAndSteps(@Param("id") Long id);

    /**
     * ✅ FIXED: Corrected query using 'workflowStep' instead of 'step'
     */
    @Query("SELECT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document " +
           "LEFT JOIN FETCH w.initiatedBy " +
           "LEFT JOIN FETCH w.tasks t " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.workflowStep " +
           "LEFT JOIN FETCH w.template " +
           "WHERE w.id = :id")
    Optional<WorkflowInstance> findByIdWithTasks(@Param("id") Long id);

    @Query("SELECT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.history " +
           "WHERE w.id = :id")
    Optional<WorkflowInstance> findByIdWithHistory(@Param("id") Long id);

    /**
     * ✅ ADDED: Enhanced method with proper ordering by createdDate
     */
    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user " +
           "ORDER BY w.createdDate DESC")
    Page<WorkflowInstance> findByInitiatedByWithDetailsOrderByCreatedDateDesc(@Param("user") User user, Pageable pageable);

    /**
     * ✅ ADDED: Date range with proper field name
     */
    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "LEFT JOIN FETCH w.tasks tasks " +
           "LEFT JOIN FETCH tasks.assignedTo " +
           "WHERE init = :user AND w.createdDate BETWEEN :from AND :to " +
           "ORDER BY w.createdDate DESC")
    Page<WorkflowInstance> findByInitiatedByAndCreatedDateBetweenWithDetails(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    // ===== SEARCH METHODS =====
    
    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.document doc " +
           "LEFT JOIN FETCH w.template tpl " +
           "LEFT JOIN FETCH w.initiatedBy init " +
           "JOIN w.document d " +
           "WHERE LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY w.startDate DESC")
    Page<WorkflowInstance> searchByDocumentNameWithDetails(@Param("q") String q, Pageable pageable);

    @Query("SELECT w FROM WorkflowInstance w " +
           "JOIN w.document d " +
           "WHERE LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<WorkflowInstance> searchByDocumentName(@Param("q") String q, Pageable pageable);

    @Query("SELECT w FROM WorkflowInstance w " +
           "JOIN w.template t " +
           "WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<WorkflowInstance> searchByTemplateName(@Param("q") String q, Pageable pageable);

    @Query("SELECT w FROM WorkflowInstance w " +
           "JOIN w.initiatedBy u " +
           "WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<WorkflowInstance> searchByInitiatorName(@Param("q") String q, Pageable pageable);

    // ===== COUNTING METHODS =====
    
    long countByStatus(WorkflowStatus status);
    long countByInitiatedBy(User user);
    long countByTemplateId(UUID templateId);
    long countByInitiatedByAndStatus(User user, WorkflowStatus status);
    long countByInitiatedByAndTemplateId(User user, UUID templateId);
    long countByInitiatedByAndStartDateBetween(User user, LocalDateTime from, LocalDateTime to);
    long countByInitiatedByAndTemplateIdAndStatus(User user, UUID templateId, WorkflowStatus status);
    long countByInitiatedByAndStartDateAfter(User user, LocalDateTime from);

    @Query("SELECT COUNT(w) FROM WorkflowInstance w WHERE w.startDate >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);

    // ===== DATE RANGE METHODS =====
    
    @Query("SELECT w FROM WorkflowInstance w WHERE w.startDate >= :from AND w.startDate <= :to")
    List<WorkflowInstance> findAllStartedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT w FROM WorkflowInstance w " +
           "LEFT JOIN FETCH w.template t " +
           "WHERE w.startDate >= :from AND w.startDate <= :to")
    List<WorkflowInstance> findAllWithTemplateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.endDate >= :from AND w.endDate <= :to")
    List<WorkflowInstance> findAllCompletedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.startDate >= :from AND w.startDate <= :to AND w.status = :status")
    List<WorkflowInstance> findByStatusAndStartDateBetween(@Param("status") WorkflowStatus status, 
                                                          @Param("from") LocalDateTime from, 
                                                          @Param("to") LocalDateTime to);

    // ===== ANALYTICS AND AGGREGATION METHODS =====
    
    @Query("SELECT w.status as status, COUNT(w) as count " +
           "FROM WorkflowInstance w " +
           "GROUP BY w.status")
    List<StatusCountProjection> countByStatusGrouped();

    @Query("SELECT w.status as status, COUNT(w) as count " +
           "FROM WorkflowInstance w " +
           "WHERE w.startDate >= :from AND w.startDate <= :to " +
           "GROUP BY w.status")
    List<StatusCountProjection> countByStatusGroupedBetween(@Param("from") LocalDateTime from, 
                                                           @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT 
            t.id::text as templateId,
            t.name as templateName,
            COUNT(w.id) as total,
            SUM(CASE WHEN w.status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
            SUM(CASE WHEN w.status = 'REJECTED' THEN 1 ELSE 0 END) as rejected,
            AVG(CASE 
                  WHEN w.end_date IS NOT NULL 
                  THEN EXTRACT(EPOCH FROM (w.end_date - w.start_date)) / 3600.0
                  ELSE NULL
                END) as avgDurationHours
        FROM workflow_instances w
        JOIN workflow_templates t ON t.id = w.template_id
        WHERE w.start_date >= :from AND w.start_date <= :to
        GROUP BY t.id, t.name
        ORDER BY total DESC
        """, nativeQuery = true)
    List<TemplateCountProjection> aggregateByTemplateBetween(@Param("from") LocalDateTime from, 
                                                           @Param("to") LocalDateTime to);

    // ===== BUSINESS LOGIC METHODS =====
    
    @Query("SELECT w FROM WorkflowInstance w WHERE w.dueDate < :now AND w.status = 'IN_PROGRESS'")
    List<WorkflowInstance> findOverdueWorkflows(@Param("now") LocalDateTime now);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.status = :status AND w.startDate < :cutoff")
    List<WorkflowInstance> findWorkflowsInStatusLongerThan(@Param("status") WorkflowStatus status, 
                                                          @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "LEFT JOIN w.tasks t " +
           "WHERE w.status = 'IN_PROGRESS' " +
           "AND (w.initiatedBy = :user OR (t.assignedTo = :user AND t.status = 'PENDING')) " +
           "ORDER BY w.startDate DESC")
    List<WorkflowInstance> findActiveWorkflowsForUser(@Param("user") User user);

    List<WorkflowInstance> findByStatusInOrderByStartDateDesc(List<WorkflowStatus> statuses);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.startDate >= :cutoff ORDER BY w.startDate DESC")
    List<WorkflowInstance> findRecentWorkflows(@Param("cutoff") LocalDateTime cutoff);

    // ===== UTILITY METHODS =====
    
    boolean existsByInitiatedByAndStatus(User user, WorkflowStatus status);
    boolean existsByTemplateId(UUID templateId);
}
