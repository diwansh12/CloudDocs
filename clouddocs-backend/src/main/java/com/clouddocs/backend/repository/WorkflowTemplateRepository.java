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
    
    // ✅ Your existing methods (might cause lazy loading issues)
    List<WorkflowTemplate> findByIsActiveTrue();
    Page<WorkflowTemplate> findByIsActiveTrue(Pageable pageable);
    Page<WorkflowTemplate> findByType(WorkflowType type, Pageable pageable);
    Page<WorkflowTemplate> findByIsActiveTrueAndType(WorkflowType type, Pageable pageable);
    Optional<WorkflowTemplate> findByName(String name);
    
    // ✅ NEW: Enhanced queries with JOIN FETCH to avoid lazy loading
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.isActive = true " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findActiveWithSteps();
    
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.id = :id")
    Optional<WorkflowTemplate> findByIdWithSteps(@Param("id") UUID id);
    
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.name = :name")
    Optional<WorkflowTemplate> findByNameWithSteps(@Param("name") String name);
    
    // ✅ Alternative: Simple query without steps for better performance
    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.isActive = true ORDER BY wt.name")
    List<WorkflowTemplate> findActiveTemplatesOnly();
}
