package com.clouddocs.backend.dto;

import com.clouddocs.backend.entity.AuditLog;   // ← your JPA entity
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lightweight projection returned by AuditLogController.
 */
@Data
public class AuditLogDTO {

    private Long id;
    private String activity;
    private String linkedItem;
    private String user;          // username / full-name
    private LocalDateTime timestamp;
    private String status;        // e.g. SUCCESS / FAILED

    /** Converts the entity to the DTO. */
    public static AuditLogDTO from(AuditLog e) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.id         = e.getId();
        dto.activity   = e.getActivity();
        dto.linkedItem = e.getLinkedItem();
        dto.user       = e.getUser();
        dto.timestamp  = e.getTimestamp();
        dto.status     = e.getStatus().name();
        return dto;
    }
}
