package com.clouddocs.backend.entity.listeners;

import com.clouddocs.backend.entity.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Slf4j
@Component
public class WorkflowInstanceListener {
    
    @PrePersist
    public void prePersist(WorkflowInstance workflow) {
        LocalDateTime now = LocalDateTime.now();
        if (workflow.getCreatedDate() == null) {
            workflow.setCreatedDate(now);
        }
        if (workflow.getUpdatedDate() == null) {
            workflow.setUpdatedDate(now);
        }
        log.debug("🔧 PrePersist: Setting timestamps for workflow {}", workflow.getId());
    }
    
    @PreUpdate
    public void preUpdate(WorkflowInstance workflow) {
        workflow.setUpdatedDate(LocalDateTime.now());
        log.info("🔧 PreUpdate: Auto-updating timestamp for workflow: {}", workflow.getId());
    }
    
    @PostPersist
    public void postPersist(WorkflowInstance workflow) {
        log.info("✅ PostPersist: Workflow {} created at {}", workflow.getId(), workflow.getCreatedDate());
    }
    
    @PostUpdate
    public void postUpdate(WorkflowInstance workflow) {
        log.info("✅ PostUpdate: Workflow {} updated at {}", workflow.getId(), workflow.getUpdatedDate());
    }
}
