package com.clouddocs.backend.entity;

import com.clouddocs.backend.converter.ERoleConverter;

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
     @Convert(converter = ERoleConverter.class)
    @Column(name = "role_name")
    private ERole roleName;

    // ✅ FIXED: Constructor now accepts ERole instead of Role
    public WorkflowStepRole(WorkflowStep step, ERole roleName) {
        this.step = step;
        this.roleName = roleName;
    }
}
