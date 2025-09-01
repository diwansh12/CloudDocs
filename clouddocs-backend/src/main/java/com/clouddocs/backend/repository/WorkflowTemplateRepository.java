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

    // ✅ Existing methods
    List<WorkflowTemplate> findByIsActiveTrue();
    Page<WorkflowTemplate> findByIsActiveTrue(Pageable pageable);
    Page<WorkflowTemplate> findByType(WorkflowType type, Pageable pageable);
    Page<WorkflowTemplate> findByIsActiveTrueAndType(WorkflowType type, Pageable pageable);
    Optional<WorkflowTemplate> findByName(String name);

    // ✅ Fetch active templates with steps only
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.isActive = true " +
           "ORDER BY wt.name")
    List<WorkflowTemplate> findActiveWithSteps();

    // ✅ Fetch template with steps (no roles) – lighter
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "WHERE wt.id = :id")
    Optional<WorkflowTemplate> findByIdWithSteps(@Param("id") UUID id);

    // ✅ Fetch template with steps + roles – avoids LazyInitializationException
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "LEFT JOIN FETCH s.roles r " +
           "WHERE wt.id = :id")
    Optional<WorkflowTemplate> findByIdWithStepsAndRoles(@Param("id") UUID id);

    // ✅ Fetch by name with steps + roles
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt " +
           "LEFT JOIN FETCH wt.steps s " +
           "LEFT JOIN FETCH s.roles r " +
           "WHERE wt.name = :name")
    Optional<WorkflowTemplate> findByNameWithStepsAndRoles(@Param("name") String name);

    // ✅ Lightweight query for dropdowns / lists
    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.isActive = true ORDER BY wt.name")
    List<WorkflowTemplate> findActiveTemplatesOnly();
}
