package com.clouddocs.backend.entity.listeners;

import com.clouddocs.backend.entity.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;
import java.time.OffsetDateTime; // ✅ UPDATED: Changed from LocalDateTime
import java.time.ZoneOffset;     // ✅ ADDED: For UTC zone offset

@Slf4j
@Component
public class WorkflowInstanceListener {
    
    @PrePersist
    public void prePersist(WorkflowInstance workflow) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC); // ✅ FIXED: Use OffsetDateTime with UTC
        if (workflow.getCreatedDate() == null) {
            workflow.setCreatedDate(now); // ✅ FIXED: Now compatible with OffsetDateTime field
        }
        if (workflow.getUpdatedDate() == null) {
            workflow.setUpdatedDate(now); // ✅ FIXED: Now compatible with OffsetDateTime field
        }
        log.debug("🔧 PrePersist: Setting timestamps for workflow {} at {}", workflow.getId(), now);
    }
    
    @PreUpdate
    public void preUpdate(WorkflowInstance workflow) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC); // ✅ FIXED: Use OffsetDateTime with UTC
        workflow.setUpdatedDate(now); // ✅ FIXED: Now compatible with OffsetDateTime field
        log.info("🔧 PreUpdate: Auto-updating timestamp for workflow {} to {}", workflow.getId(), now);
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
