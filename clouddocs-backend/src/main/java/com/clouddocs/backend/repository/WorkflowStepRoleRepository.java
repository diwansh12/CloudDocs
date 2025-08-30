package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.WorkflowStepRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepRoleRepository extends JpaRepository<WorkflowStepRole, Long> {
    List<WorkflowStepRole> findByStepId(Long stepId);
}
