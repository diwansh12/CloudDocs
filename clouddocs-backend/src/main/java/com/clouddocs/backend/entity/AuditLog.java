package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Records every significant action for the Audit-Trail page.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable action (e.g. “Document Uploaded”). */
    @Column(nullable = false, length = 255)
    private String activity;

    /** Optional object the action relates to (“Project-Plan.pdf”). */
    @Column(name = "linked_item")
    private String linkedItem;

    /** Username or full-name of the actor. */
@Column(name = "user_name", nullable = false, length = 255)
private String user;

    /** When the action happened. */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** SUCCESS / FAILED / WARN … */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /* ----- helper factory ----- */
    public static AuditLog of(String activity,
                              String linkedItem,
                              String user,
                              Status status) {

        return AuditLog.builder()
                       .activity(activity)
                       .linkedItem(linkedItem)
                       .user(user)
                       .timestamp(LocalDateTime.now())
                       .status(status)
                       .build();
    }

    /* ----- enum ----- */
    public enum Status {
        SUCCESS, FAILED
    }
}
