package com.clouddocs.backend.config;

import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               RoleRepository roleRepository,
                               WorkflowTemplateRepository templateRepository,
                               WorkflowStepRepository stepRepository,
                               WorkflowStepRoleRepository stepRoleRepository,
                               WorkflowInstanceRepository instanceRepository,
                               WorkflowTaskRepository taskRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {

            System.out.println("🚀 Starting data initialization...");

            // ✅ STEP 1: Initialize Roles first
            initializeRoles(roleRepository);

            // ✅ STEP 2: Create default user with proper Many-to-Many role assignment
            User approver = userRepository.findByUsername("diwansh12").orElse(null);

            if (approver == null) {
                // ✅ FIXED: Use correct enum constant (ROLE_MANAGER, not MANAGER)
                Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                    .orElseThrow(() -> new RuntimeException("MANAGER role not found in database"));

                approver = new User();
                approver.setUsername("diwansh12");
                approver.setEmail("diwansh1112@gmail.com");
                approver.setPassword(passwordEncoder.encode("Diwansh@123"));
                approver.setFirstName("Diwansh");
                approver.setLastName("Sood");
                
                // ✅ Set roles using mutable HashSet
                Set<Role> roles = new HashSet<>();
                roles.add(managerRole);
                approver.setRoles(roles);
                
                approver.setActive(true);
                approver.setEnabled(true);
                
                approver = userRepository.save(approver);
                System.out.println("✅ Created default user: " + approver.getUsername() + " with roles: " + 
                    approver.getRoles().stream().map(r -> r.getName().name()).toList());
            } else {
                System.out.println("✅ Default user already exists: " + approver.getUsername());
            }

            // ✅ STEP 3: Initialize workflows only if they don't exist
            if (templateRepository.count() == 0) {
                initializeWorkflowTemplates(templateRepository, stepRepository, stepRoleRepository, roleRepository, approver);
            } else {
                System.out.println("✅ Workflow templates already exist, skipping initialization");
            }

            System.out.println("🎉 Data initialization completed successfully!");
        };
    }

    /**
     * ✅ FIXED: Initialize roles with correct enum constants and existence check
     */
    private void initializeRoles(RoleRepository roleRepository) {
        System.out.println("🔧 Initializing roles...");
        
        // ✅ FIXED: Check if roles already exist to prevent duplicates
        if (roleRepository.count() > 0) {
            System.out.println("✅ Roles already exist, skipping role initialization");
            return;
        }
        
        // ✅ FIXED: Use correct enum constants (ROLE_ADMIN, ROLE_MANAGER, ROLE_USER)
        ERole[] rolesToCreate = {ERole.ROLE_ADMIN, ERole.ROLE_MANAGER, ERole.ROLE_USER};
        
        for (ERole eRole : rolesToCreate) {
            if (!roleRepository.findByName(eRole).isPresent()) {
                Role role = new Role();
                role.setName(eRole);
                role.setDescription(getDefaultRoleDescription(eRole));
                roleRepository.save(role);
                System.out.println("✅ Created role: " + eRole + " - " + role.getDescription());
            } else {
                System.out.println("✅ Role already exists: " + eRole);
            }
        }
    }

    /**
     * ✅ FIXED: Get default description using correct enum constants
     */
    private String getDefaultRoleDescription(ERole eRole) {
        return switch (eRole) {
            case ROLE_ADMIN -> "Administrator with full system access";
            case ROLE_MANAGER -> "Manager with workflow approval rights";
            case ROLE_USER -> "Regular user with basic access";
        };
    }

    /**
     * ✅ FIXED: Complete workflow initialization with correct enum constants
     */
    private void initializeWorkflowTemplates(WorkflowTemplateRepository templateRepository,
                                           WorkflowStepRepository stepRepository,
                                           WorkflowStepRoleRepository stepRoleRepository,
                                           RoleRepository roleRepository,
                                           User approver) {
        
        System.out.println("🔧 Initializing workflow templates...");

        // ✅ Create a Workflow Template
        WorkflowTemplate template = new WorkflowTemplate();
        template.setName("Default Approval Workflow");
        template.setDescription("Basic document approval workflow");
        template.setType(WorkflowType.DOCUMENT_APPROVAL);
        template.setDefaultSlaHours(48);
        template.setIsActive(true);
        
        // ✅ Initialize steps collection
        template.setSteps(new LinkedHashSet<>());
        
        template = templateRepository.save(template);
        System.out.println("✅ Created workflow template: " + template.getName());

        // ✅ FIXED: Fetch roles using correct enum constants
        Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
            .orElseThrow(() -> new RuntimeException("MANAGER role not found"));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
            .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

        // ✅ FIXED: Step 1 creation with proper relationships
        WorkflowStep step1 = new WorkflowStep("Initial Review", 1, StepType.REVIEW);
        step1.setTemplate(template);
        step1.setDescription("First level document review");
        step1.setApprovalPolicy(ApprovalPolicy.QUORUM);
        step1.setRequiredApprovals(1);
        step1.setSlaHours(24);
        
        // ✅ Use mutable HashSet
        Set<User> approvers1 = new HashSet<>();
        approvers1.add(approver);
        step1.setAssignedApprovers(approvers1);
        
        // ✅ Initialize roles collection
        step1.setRoles(new HashSet<>());
        
        step1 = stepRepository.save(step1);
        
        // ✅ FIXED: Create step role relationship with correct enum constant
        WorkflowStepRole step1Role = new WorkflowStepRole(step1, managerRole.getName());
        stepRoleRepository.save(step1Role);
        
        // ✅ Add step to template's collection
        template.getSteps().add(step1);
        
        System.out.println("✅ Created step 1 '" + step1.getName() + "' with " + 
            step1.getAssignedApprovers().size() + " direct approvers and role: " + managerRole.getName());

        // ✅ FIXED: Step 2 creation with proper relationships  
        WorkflowStep step2 = new WorkflowStep("Final Approval", 2, StepType.APPROVAL);
        step2.setTemplate(template);
        step2.setDescription("Final document approval by admin");
        step2.setApprovalPolicy(ApprovalPolicy.ALL);
        step2.setRequiredApprovals(1);
        step2.setSlaHours(24);
        
        // ✅ Use mutable HashSet
        Set<User> approvers2 = new HashSet<>();
        approvers2.add(approver);
        step2.setAssignedApprovers(approvers2);
        
        // ✅ Initialize roles collection
        step2.setRoles(new HashSet<>());
        
        step2 = stepRepository.save(step2);
        
        // ✅ FIXED: Create step role relationship with correct enum constant
        WorkflowStepRole step2Role = new WorkflowStepRole(step2, adminRole.getName());
        stepRoleRepository.save(step2Role);
        
        // ✅ Add step to template's collection
        template.getSteps().add(step2);
        
        System.out.println("✅ Created step 2 '" + step2.getName() + "' with " + 
            step2.getAssignedApprovers().size() + " direct approvers and role: " + adminRole.getName());

        // ✅ Save template with complete step relationships
        templateRepository.save(template);
        
        System.out.println("✅ Workflow template saved with " + template.getSteps().size() + " steps");
        System.out.println("✅ Workflow initialization completed successfully!");
    }
}
