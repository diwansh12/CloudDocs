package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.WorkflowTemplateDto;
import com.clouddocs.backend.dto.workflow.WorkflowStepDTO;
import com.clouddocs.backend.entity.WorkflowTemplate;
import com.clouddocs.backend.entity.WorkflowStep;
import com.clouddocs.backend.entity.WorkflowType;
import com.clouddocs.backend.repository.WorkflowTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/workflow-templates")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class WorkflowTemplateController {

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    // FIXED: Return DTOs instead of entities to avoid circular reference
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping
    public ResponseEntity<List<WorkflowTemplateDto>> listTemplates(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String type) {

        try {
            List<WorkflowTemplate> templates = templateRepository.findAll();

            if (active != null) {
                templates = templates.stream().filter(t -> active.equals(t.getIsActive())).toList();
            }
            if (type != null) {
                try {
                    WorkflowType workflowType = WorkflowType.valueOf(type.toUpperCase());
                    templates = templates.stream().filter(t -> workflowType.equals(t.getType())).toList();
                } catch (IllegalArgumentException e) {
                    templates = List.of();
                }
            }

            // Convert entities to DTOs
            List<WorkflowTemplateDto> templateDTOs = templates.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(templateDTOs);
        } catch (Exception e) {
            System.err.println("Error in listTemplates: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch templates", e);
        }
    }

    // FIXED: Return Page<WorkflowTemplateDto> instead of Page<WorkflowTemplate>
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/paged")
    public ResponseEntity<Page<WorkflowTemplateDto>> listTemplatesPaged(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<WorkflowTemplate> result;
            if (active != null && active && type != null) {
                try {
                    WorkflowType workflowType = WorkflowType.valueOf(type.toUpperCase());
                    result = templateRepository.findByIsActiveTrueAndType(workflowType, pageable);
                } catch (IllegalArgumentException e) {
                    result = Page.empty(pageable);
                }
            } else if (active != null && active) {
                result = templateRepository.findByIsActiveTrue(pageable);
            } else if (type != null) {
                try {
                    WorkflowType workflowType = WorkflowType.valueOf(type.toUpperCase());
                    result = templateRepository.findByType(workflowType, pageable);
                } catch (IllegalArgumentException e) {
                    result = Page.empty(pageable);
                }
            } else {
                result = templateRepository.findAll(pageable);
            }

            // Convert Page<WorkflowTemplate> to Page<WorkflowTemplateDto>
            Page<WorkflowTemplateDto> dtoPage = result.map(this::convertToDTO);

            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            System.err.println("Error in listTemplatesPaged: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch paged templates", e);
        }
    }

    // FIXED: Return WorkflowTemplateDto instead of WorkflowTemplate
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowTemplateDto> getTemplate(@PathVariable UUID id) {
        try {
            WorkflowTemplate template = templateRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));
            
            WorkflowTemplateDto dto = convertToDTO(template);
            return ResponseEntity.ok(dto);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error in getTemplate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch template", e);
        }
    }

    // FIXED: The main endpoint causing circular reference - now returns DTOs
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/active")
    public ResponseEntity<List<WorkflowTemplateDto>> getActiveTemplates() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("=== GET ACTIVE TEMPLATES ===");
            System.out.println("User: " + auth.getName());
            System.out.println("Authorities: " + auth.getAuthorities());
            
            List<WorkflowTemplate> activeTemplates = templateRepository.findByIsActiveTrue();
            System.out.println("Active templates found: " + activeTemplates.size());
            
            // FIXED: Convert entities to DTOs to avoid circular reference
            List<WorkflowTemplateDto> templateDTOs = activeTemplates.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            System.out.println("Converted to DTOs: " + templateDTOs.size());
            
            return ResponseEntity.ok(templateDTOs);
            
        } catch (Exception e) {
            System.err.println("Error in getActiveTemplates: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch active templates", e);
        }
    }

    // FIXED: Return WorkflowTemplateDto instead of WorkflowTemplate
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/by-name/{name}")
    public ResponseEntity<WorkflowTemplateDto> getTemplateByName(@PathVariable String name) {
        try {
            WorkflowTemplate template = templateRepository.findByName(name)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found with name: " + name));
            
            WorkflowTemplateDto dto = convertToDTO(template);
            return ResponseEntity.ok(dto);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error in getTemplateByName: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch template by name", e);
        }
    }

    // ADDED: Create new template endpoint
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<WorkflowTemplateDto> createTemplate(@RequestBody WorkflowTemplateDto templateDto) {
        try {
            WorkflowTemplate template = convertFromDTO(templateDto);
            WorkflowTemplate savedTemplate = templateRepository.save(template);
            WorkflowTemplateDto responseDto = convertToDTO(savedTemplate);
            
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            System.err.println("Error in createTemplate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create template", e);
        }
    }

    // ADDED: Update template endpoint
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowTemplateDto> updateTemplate(@PathVariable UUID id, @RequestBody WorkflowTemplateDto templateDto) {
        try {
            WorkflowTemplate existingTemplate = templateRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));
            
            // Update fields
            existingTemplate.setName(templateDto.getName());
            existingTemplate.setDescription(templateDto.getDescription());
            existingTemplate.setIsActive(templateDto.getIsActive());
            if (templateDto.getType() != null) {
                existingTemplate.setType(WorkflowType.valueOf(templateDto.getType()));
            }
            existingTemplate.setDefaultSlaHours(templateDto.getDefaultSlaHours());
            
            WorkflowTemplate savedTemplate = templateRepository.save(existingTemplate);
            WorkflowTemplateDto responseDto = convertToDTO(savedTemplate);
            
            return ResponseEntity.ok(responseDto);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error in updateTemplate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update template", e);
        }
    }

    // ADDED: Delete template endpoint
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        try {
            if (!templateRepository.existsById(id)) {
                throw new ResponseStatusException(NOT_FOUND, "Template not found");
            }
            
            templateRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error in deleteTemplate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete template", e);
        }
    }

    /**
     * Convert WorkflowTemplate entity to WorkflowTemplateDto
     * This method breaks the circular reference by not including template references in steps
     */
    private WorkflowTemplateDto convertToDTO(WorkflowTemplate template) {
        // Create the main DTO with extended constructor
        WorkflowTemplateDto dto = new WorkflowTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setIsActive(template.getIsActive());
        dto.setType(template.getType() != null ? template.getType().name() : null);
        dto.setDefaultSlaHours(template.getDefaultSlaHours());
        
        // Convert steps to DTOs (without template reference to break circular dependency)
        if (template.getSteps() != null && !template.getSteps().isEmpty()) {
            List<WorkflowStepDTO> stepDTOs = template.getSteps().stream()
                    .map(this::convertStepToDTO)
                    .sorted((s1, s2) -> Integer.compare(
                        s1.getStepOrder() != null ? s1.getStepOrder() : 0, 
                        s2.getStepOrder() != null ? s2.getStepOrder() : 0))
                    .collect(Collectors.toList());
            dto.setSteps(stepDTOs);
        }
        
        return dto;
    }

    /**
     * Convert WorkflowStep entity to WorkflowStepDTO
     */
    private WorkflowStepDTO convertStepToDTO(WorkflowStep step) {
        WorkflowStepDTO stepDTO = new WorkflowStepDTO();
        stepDTO.setId(step.getId());
        stepDTO.setName(step.getName());
        stepDTO.setStepOrder(step.getStepOrder());
        stepDTO.setStepType(step.getStepType());
        stepDTO.setSlaHours(step.getSlaHours());
        
        // Get required roles for this step
        // Adjust this logic based on how you store step roles in your system
        if (step.getRequiredRoles() != null && !step.getRequiredRoles().isEmpty()) {
            List<String> roleNames = step.getRequiredRoles().stream()
                    .map(role -> role.name())
                    .collect(Collectors.toList());
            stepDTO.setRequiredRoles(roleNames);
        }
        
        return stepDTO;
    }

    /**
     * Convert WorkflowTemplateDto to WorkflowTemplate entity (for create/update operations)
     */
    private WorkflowTemplate convertFromDTO(WorkflowTemplateDto dto) {
        WorkflowTemplate template = new WorkflowTemplate();
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        if (dto.getType() != null) {
            template.setType(WorkflowType.valueOf(dto.getType()));
        }
        template.setDefaultSlaHours(dto.getDefaultSlaHours());
        
        // Note: Steps are typically handled separately in a more complex workflow management system
        // You might want to add step conversion logic here if needed
        
        return template;
    }
}


