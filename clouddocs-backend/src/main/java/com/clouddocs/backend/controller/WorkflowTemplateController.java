package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.WorkflowTemplateDto;
import com.clouddocs.backend.dto.workflow.WorkflowStepDTO;
import com.clouddocs.backend.entity.WorkflowTemplate;
import com.clouddocs.backend.entity.WorkflowStep;
import com.clouddocs.backend.entity.WorkflowType;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.repository.WorkflowTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/workflow-templates")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
public class WorkflowTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowTemplateController.class);

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    /**
     * ✅ MAIN FIX: Active templates endpoint with proper error handling
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/active")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowTemplateDto>> getActiveTemplates() {
        try {
            logger.info("🔍 Getting active workflow templates");
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.debug("User: {}, Authorities: {}", auth.getName(), auth.getAuthorities());
            
            List<WorkflowTemplate> activeTemplates = templateRepository.findByIsActiveTrue();
            logger.info("Active templates found: {}", activeTemplates.size());
            
            List<WorkflowTemplateDto> templateDTOs = activeTemplates.stream()
                    .map(this::convertToDTOSafe)
                    .filter(Objects::nonNull)  // Remove failed conversions
                    .collect(Collectors.toList());
            
            logger.info("✅ Successfully converted {} templates to DTOs", templateDTOs.size());
            
            return ResponseEntity.ok(templateDTOs);
            
        } catch (Exception e) {
            logger.error("❌ Error in getActiveTemplates: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * List templates with optional filtering
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowTemplateDto>> listTemplates(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String type) {

        try {
            logger.info("🔍 Listing templates - active: {}, type: {}", active, type);
            
            List<WorkflowTemplate> templates = templateRepository.findAll();

            // Apply filters
            if (active != null) {
                templates = templates.stream()
                        .filter(t -> active.equals(t.getIsActive()))
                        .collect(Collectors.toList());
            }
            
            if (type != null) {
                try {
                    WorkflowType workflowType = WorkflowType.valueOf(type.toUpperCase());
                    templates = templates.stream()
                            .filter(t -> workflowType.equals(t.getType()))
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid workflow type: {}", type);
                    templates = List.of();
                }
            }

            List<WorkflowTemplateDto> templateDTOs = templates.stream()
                    .map(this::convertToDTOSafe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            logger.info("✅ Successfully listed {} templates", templateDTOs.size());
            return ResponseEntity.ok(templateDTOs);

        } catch (Exception e) {
            logger.error("❌ Error in listTemplates: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * List templates with pagination and filtering
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/paged")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<WorkflowTemplateDto>> listTemplatesPaged(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            logger.info("🔍 Getting paged templates - page: {}, size: {}", page, size);
            
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

            Page<WorkflowTemplateDto> dtoPage = result.map(this::convertToDTOSafe);
            
            logger.info("✅ Successfully returned paged templates: {} of {}", 
                       dtoPage.getNumberOfElements(), dtoPage.getTotalElements());

            return ResponseEntity.ok(dtoPage);

        } catch (Exception e) {
            logger.error("❌ Error in listTemplatesPaged: {}", e.getMessage(), e);
            return ResponseEntity.ok(Page.empty());
        }
    }

    /**
     * Get template by ID
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowTemplateDto> getTemplate(@PathVariable UUID id) {
        try {
            logger.info("🔍 Getting template by ID: {}", id);
            
            WorkflowTemplate template = templateRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));
            
            WorkflowTemplateDto dto = convertToDTOSafe(template);
            if (dto == null) {
                throw new ResponseStatusException(NOT_FOUND, "Template could not be processed");
            }
            
            logger.info("✅ Successfully retrieved template: {}", template.getName());
            return ResponseEntity.ok(dto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error in getTemplate: {}", e.getMessage(), e);
            throw new ResponseStatusException(NOT_FOUND, "Template not found");
        }
    }

    /**
     * Get template by name
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/by-name/{name}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowTemplateDto> getTemplateByName(@PathVariable String name) {
        try {
            logger.info("🔍 Getting template by name: {}", name);
            
            WorkflowTemplate template = templateRepository.findByName(name)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found with name: " + name));
            
            WorkflowTemplateDto dto = convertToDTOSafe(template);
            if (dto == null) {
                throw new ResponseStatusException(NOT_FOUND, "Template could not be processed");
            }
            
            logger.info("✅ Successfully retrieved template by name: {}", name);
            return ResponseEntity.ok(dto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error in getTemplateByName: {}", e.getMessage(), e);
            throw new ResponseStatusException(NOT_FOUND, "Template not found with name: " + name);
        }
    }

    /**
     * Create new template
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<WorkflowTemplateDto> createTemplate(@RequestBody WorkflowTemplateDto templateDto) {
        try {
            logger.info("🔍 Creating new template: {}", templateDto.getName());
            
            WorkflowTemplate template = convertFromDTO(templateDto);
            WorkflowTemplate savedTemplate = templateRepository.save(template);
            WorkflowTemplateDto responseDto = convertToDTOSafe(savedTemplate);
            
            logger.info("✅ Successfully created template: {}", savedTemplate.getName());
            return ResponseEntity.ok(responseDto);
            
        } catch (Exception e) {
            logger.error("❌ Error in createTemplate: {}", e.getMessage(), e);
            throw new ResponseStatusException(NOT_FOUND, "Failed to create template");
        }
    }

    /**
     * Update existing template
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<WorkflowTemplateDto> updateTemplate(@PathVariable UUID id, @RequestBody WorkflowTemplateDto templateDto) {
        try {
            logger.info("🔍 Updating template: {}", id);
            
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
            WorkflowTemplateDto responseDto = convertToDTOSafe(savedTemplate);
            
            logger.info("✅ Successfully updated template: {}", savedTemplate.getName());
            return ResponseEntity.ok(responseDto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error in updateTemplate: {}", e.getMessage(), e);
            throw new ResponseStatusException(NOT_FOUND, "Failed to update template");
        }
    }

    /**
     * Delete template
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        try {
            logger.info("🔍 Deleting template: {}", id);
            
            if (!templateRepository.existsById(id)) {
                throw new ResponseStatusException(NOT_FOUND, "Template not found");
            }
            
            templateRepository.deleteById(id);
            
            logger.info("✅ Successfully deleted template: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error in deleteTemplate: {}", e.getMessage(), e);
            throw new ResponseStatusException(NOT_FOUND, "Failed to delete template");
        }
    }

    /**
     * ✅ SAFE DTO conversion with comprehensive error handling
     */
    private WorkflowTemplateDto convertToDTOSafe(WorkflowTemplate template) {
        try {
            WorkflowTemplateDto dto = new WorkflowTemplateDto();
            
            // Basic fields - no lazy loading issues
            dto.setId(template.getId());
            dto.setName(template.getName());
            dto.setDescription(template.getDescription());
            dto.setIsActive(template.getIsActive());
            dto.setType(template.getType() != null ? template.getType().name() : null);
            dto.setDefaultSlaHours(template.getDefaultSlaHours());
            
            // ✅ Safe handling of lazy-loaded steps collection
            try {
                if (template.getSteps() != null && !template.getSteps().isEmpty()) {
                    List<WorkflowStepDTO> stepDTOs = template.getSteps().stream()
                            .map(this::convertStepToDTOSafe)
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparingInt(s -> s.getStepOrder() != null ? s.getStepOrder() : 0))
                            .collect(Collectors.toList());
                    dto.setSteps(stepDTOs);
                } else {
                    dto.setSteps(new ArrayList<>());
                }
            } catch (Exception e) {
                logger.warn("⚠️ Could not load steps for template {}: {}", template.getId(), e.getMessage());
                dto.setSteps(new ArrayList<>());
            }
            
            return dto;
            
        } catch (Exception e) {
            logger.error("❌ Error converting template {} to DTO: {}", 
                        template != null ? template.getId() : "null", e.getMessage());
            return null;  // This will be filtered out in the stream
        }
    }

    /**
     * ✅ FIXED: Safe step DTO conversion with Many-to-Many role support
     */
    private WorkflowStepDTO convertStepToDTOSafe(WorkflowStep step) {
        try {
            WorkflowStepDTO stepDTO = new WorkflowStepDTO();
            stepDTO.setId(step.getId());
            stepDTO.setName(step.getName());
            stepDTO.setStepOrder(step.getStepOrder());
            stepDTO.setStepType(step.getStepType());
            stepDTO.setSlaHours(step.getSlaHours());
            
            // ✅ FIXED: Handle Many-to-Many roles properly
            try {
                if (step.getRequiredRoles() != null && !step.getRequiredRoles().isEmpty()) {
                    // ✅ FIXED: Explicit lambda parameter type to resolve inference error
                    List<String> roleNames = step.getRequiredRoles().stream()
                            .map((Role role) -> role.getName().name()) // ✅ FIXED: Use getName().name() for Role entity
                            .collect(Collectors.toList());
                    stepDTO.setRequiredRoles(roleNames);
                } else {
                    stepDTO.setRequiredRoles(new ArrayList<>());
                }
            } catch (Exception e) {
                logger.warn("⚠️ Could not load roles for step {}: {}", step.getId(), e.getMessage());
                stepDTO.setRequiredRoles(new ArrayList<>());
            }
            
            return stepDTO;
            
        } catch (Exception e) {
            logger.error("❌ Error converting step {} to DTO: {}", 
                        step != null ? step.getId() : "null", e.getMessage());
            return null;
        }
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

    /**
     * ✅ NEW: Get template statistics
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTemplateStatistics() {
        try {
            logger.info("📊 Getting template statistics");
            
            List<WorkflowTemplate> allTemplates = templateRepository.findAll();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTemplates", allTemplates.size());
            stats.put("activeTemplates", allTemplates.stream().mapToLong(t -> t.getIsActive() ? 1 : 0).sum());
            stats.put("inactiveTemplates", allTemplates.stream().mapToLong(t -> !t.getIsActive() ? 1 : 0).sum());
            
            // Group by type
            Map<String, Long> byType = allTemplates.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getType() != null ? t.getType().name() : "UNKNOWN",
                    Collectors.counting()
                ));
            stats.put("templatesByType", byType);
            
            // Average steps per template
            double avgSteps = allTemplates.stream()
                .mapToInt(t -> t.getSteps() != null ? t.getSteps().size() : 0)
                .average()
                .orElse(0.0);
            stats.put("averageStepsPerTemplate", Math.round(avgSteps * 100.0) / 100.0);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("❌ Error getting template statistics: {}", e.getMessage(), e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }
}
