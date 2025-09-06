package com.clouddocs.backend.config;

import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               WorkflowTemplateRepository templateRepository,
                               WorkflowStepRepository stepRepository,
                               WorkflowStepRoleRepository stepRoleRepository,
                               WorkflowInstanceRepository instanceRepository,
                               WorkflowTaskRepository taskRepository) {
        return args -> {

            // ✅ Ensure we always have at least one user
            User approver = userRepository.findByUsername("diwansh12").orElse(null);

            if (approver == null) {
                approver = new User();
                approver.setUsername("diwansh12");
                approver.setEmail("diwansh1112@gmail.com");
                approver.setPassword("Diwansh@123"); 
                approver.setRole(Role.MANAGER);
                approver = userRepository.save(approver);
                System.out.println("✅ Created default user: " + approver.getUsername());
            }

            // 🚫 REMOVED: Dangerous cleanup section that was deleting all workflows

            // ✅ Only create templates and steps if they don't exist
            if (templateRepository.count() == 0) {
                // ✅ Create a Workflow Template
                WorkflowTemplate template = new WorkflowTemplate();
                template.setName("Default Approval Workflow");
                template.setDescription("Basic document approval workflow");
                template.setType(WorkflowType.DOCUMENT_APPROVAL);
                template.setDefaultSlaHours(48);
                template.setIsActive(true);
                template = templateRepository.save(template);
                System.out.println("✅ Created workflow template: " + template.getName());

                // ✅ Step 1: Review
                WorkflowStep step1 = new WorkflowStep("Initial Review", 1, StepType.REVIEW);
                step1.setTemplate(template);
                step1.setDescription("First level document review");
                step1.setApprovalPolicy(ApprovalPolicy.QUORUM);
                step1.setRequiredApprovals(1);
                step1.setSlaHours(24);
                step1.setAssignedApprovers(List.of(approver));
                step1 = stepRepository.save(step1);
                
                WorkflowStepRole step1Role = new WorkflowStepRole(step1, Role.MANAGER);
                stepRoleRepository.save(step1Role);
                System.out.println("✅ Created step 1 with " + step1.getAssignedApprovers().size() + " direct approvers");

                // ✅ Step 2: Final Approval
                WorkflowStep step2 = new WorkflowStep("Final Approval", 2, StepType.APPROVAL);
                step2.setTemplate(template);
                step2.setDescription("Final document approval by admin");
                step2.setApprovalPolicy(ApprovalPolicy.ALL);
                step2.setRequiredApprovals(1);
                step2.setSlaHours(24);
                step2.setAssignedApprovers(List.of(approver));
                step2 = stepRepository.save(step2);
                
                WorkflowStepRole step2Role = new WorkflowStepRole(step2, Role.ADMIN);
                stepRoleRepository.save(step2Role);
                System.out.println("✅ Created step 2 with " + step2.getAssignedApprovers().size() + " direct approvers");

                System.out.println("✅ Workflow initialization completed successfully!");
            } else {
                System.out.println("✅ Workflow templates already exist, skipping initialization");
            }
        };
    }
}
