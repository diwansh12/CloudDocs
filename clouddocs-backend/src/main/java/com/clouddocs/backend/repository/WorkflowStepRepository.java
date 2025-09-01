package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.WorkflowStep;
import com.clouddocs.backend.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    // ✅ Existing method
    List<WorkflowStep> findByTemplateOrderByStepOrderAsc(WorkflowTemplate template);

    // ✅ New method: fetch steps with roles for a given template
    @Query("SELECT DISTINCT s FROM WorkflowStep s " +
           "LEFT JOIN FETCH s.roles r " +
           "WHERE s.template.id = :templateId " +
           "ORDER BY s.stepOrder ASC")
    List<WorkflowStep> findStepsWithRoles(@Param("templateId") UUID templateId);
}

