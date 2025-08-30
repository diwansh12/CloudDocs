package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.WorkflowTemplate;
import com.clouddocs.backend.entity.WorkflowType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {
    
    List<WorkflowTemplate> findByIsActiveTrue();
    
    Page<WorkflowTemplate> findByIsActiveTrue(Pageable pageable);
    
    Page<WorkflowTemplate> findByType(WorkflowType type, Pageable pageable);
    
    Page<WorkflowTemplate> findByIsActiveTrueAndType(WorkflowType type, Pageable pageable);
    
    // Optional: find by name for easier template selection
    Optional<WorkflowTemplate> findByName(String name);
}
