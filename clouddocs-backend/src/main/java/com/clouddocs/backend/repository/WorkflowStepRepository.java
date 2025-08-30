package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.WorkflowStep;
import com.clouddocs.backend.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
List<WorkflowStep> findByTemplateOrderByStepOrderAsc(WorkflowTemplate template);
}
