package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.WorkflowHistory;
import com.clouddocs.backend.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {
List<WorkflowHistory> findByWorkflowInstanceOrderByActionDateAsc(WorkflowInstance instance);
}