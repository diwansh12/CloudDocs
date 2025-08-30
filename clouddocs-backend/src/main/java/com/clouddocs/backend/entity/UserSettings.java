package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Per-user preference bundle persisted in a single row.
 */
@Entity
@Table(name = "user_settings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------- One-to-one with the User ------------- */
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /* ---------- General ------------------------------ */
    @Column(name = "app_name", length = 80, nullable = false)
    private String appName;

    @Column(name = "timezone", length = 64, nullable = false)
    private String timezone;

    @Column(name = "language", length = 32, nullable = false)
    private String language;

    /* ---------- Security ----------------------------- */
    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @Size(max = 120)
    @Column(name = "password_policy", nullable = false)
    private String passwordPolicy;

    /* ---------- Notification channels ---------------- */
    @Column(name = "email_approval", nullable = false)
    private boolean emailApproval;

    @Column(name = "sms_enabled", nullable = false)
    private boolean sms;

    @Column(name = "push_enabled", nullable = false)
    private boolean push;
}
