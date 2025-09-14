package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.WorkflowTemplate;
import com.clouddocs.backend.entity.WorkflowType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    // ===== BASIC QUERIES (No Fetching) =====
    
    /**
     * Find active templates - basic query without fetching steps
     * Use when you only need template metadata
     */
    List<WorkflowTemplate> findByIsActiveTrue();
    
    /**
     * Find active templates with pagination
     */
    Page<WorkflowTemplate> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Find templates by type with pagination
     */
    Page<WorkflowTemplate> findByType(WorkflowType type, Pageable pageable);
    
    /**
     * Find active templates by type with pagination
     */
    Page<WorkflowTemplate> findByIsActiveTrueAndType(WorkflowType type, Pageable pageable);
    
    /**
     * Find template by name - basic query
     */
    Optional<WorkflowTemplate> findByName(String name);

    // ===== FETCH JOIN QUERIES (Steps Only) =====
    
    /**
     * ✅ FIXED: Consistent naming - find active templates with steps eagerly loaded
     * Use for template listings that need step counts
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.isActive = true " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findActiveTemplatesWithSteps();
    
    /**
     * ✅ NEW: Find all templates with steps (active and inactive)
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findAllTemplatesWithSteps();

    /**
     * Find template by ID with steps eagerly loaded
     * Use when you need template details with step information
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.id = :id")
    Optional<WorkflowTemplate> findByIdWithSteps(@Param("id") UUID id);

    /**
     * Find template by name with steps eagerly loaded
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.name = :name")
    Optional<WorkflowTemplate> findByNameWithSteps(@Param("name") String name);
    
    /**
     * ✅ NEW: Find templates by type with steps
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.type = :type " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findByTypeWithSteps(@Param("type") WorkflowType type);

    // ===== FETCH JOIN QUERIES (Steps + Roles) =====
    
    /**
     * ✅ ENHANCED: Find active templates with steps and roles
     * Use sparingly - only when you need complete template data
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "LEFT JOIN FETCH s.roles r " +
           "WHERE wt.isActive = true " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findActiveTemplatesWithStepsAndRoles();

    /**
     * Find template by ID with complete data (steps + roles)
     * Use for template editing/detailed view
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "LEFT JOIN FETCH s.roles r " +
           "WHERE wt.id = :id")
    Optional<WorkflowTemplate> findByIdWithStepsAndRoles(@Param("id") UUID id);

    /**
     * Find template by name with complete data (steps + roles)
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "LEFT JOIN FETCH s.roles r " +
           "WHERE wt.name = :name")
    Optional<WorkflowTemplate> findByNameWithStepsAndRoles(@Param("name") String name);

    // ===== LIGHTWEIGHT QUERIES =====
    
    /**
     * ✅ ENHANCED: Lightweight query for dropdowns/selectors
     * Only fetches template metadata - no steps
     */
    @Query("SELECT wt FROM WorkflowTemplate wt " +
           "WHERE wt.isActive = true " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findActiveTemplatesLightweight();
    
    /**
     * ✅ NEW: Get template names and IDs only
     */
    @Query("SELECT wt.id, wt.name FROM WorkflowTemplate wt " +
           "WHERE wt.isActive = true " +
           "ORDER BY wt.name")
    List<Object[]> findActiveTemplateNamesAndIds();

    // ===== SEARCH AND FILTER QUERIES =====
    
    /**
     * ✅ NEW: Search templates by name pattern
     */
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.isActive = true " +
           "AND LOWER(wt.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> searchActiveTemplatesByName(@Param("searchTerm") String searchTerm);
    
    /**
     * ✅ NEW: Find templates with specific step count
     */
    @Query("SELECT wt FROM WorkflowTemplate wt " +
           "WHERE wt.isActive = true " +
           "AND SIZE(wt.steps) = :stepCount")
    List<WorkflowTemplate> findActiveTemplatesWithStepCount(@Param("stepCount") int stepCount);

    // ===== STATISTICS QUERIES =====
    
    /**
     * ✅ NEW: Count active templates by type
     */
    @Query("SELECT wt.type, COUNT(wt) FROM WorkflowTemplate wt " +
           "WHERE wt.isActive = true " +
           "GROUP BY wt.type")
    List<Object[]> countActiveTemplatesByType();
    
    /**
     * ✅ NEW: Find templates with no steps (incomplete templates)
     */
    @Query("SELECT wt FROM WorkflowTemplate wt " +
           "WHERE SIZE(wt.steps) = 0")
    List<WorkflowTemplate> findTemplatesWithoutSteps();
}
