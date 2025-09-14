package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_step_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private WorkflowStep step;

    // ✅ FIXED: Using ERole enum
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private ERole roleName;

    // ✅ FIXED: Constructor now accepts ERole instead of Role
    public WorkflowStepRole(WorkflowStep step, ERole roleName) {
        this.step = step;
        this.roleName = roleName;
    }
}
