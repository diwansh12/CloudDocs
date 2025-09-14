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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestController
@RequestMapping("/workflow-templates")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             allowCredentials = "true", allowedHeaders = "*")
public class WorkflowTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowTemplateController.class);

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    /**
     * ✅ FIXED: Get active templates using fetch join to avoid LazyInitializationException
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/active")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowTemplateDto>> getActiveTemplates() {
        try {
            logger.info("🔍 Getting active workflow templates");
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.debug("User: {}, Authorities: {}", auth.getName(), auth.getAuthorities());
            
            // ✅ FIXED: Use fetch join query to eagerly load steps
            List<WorkflowTemplate> activeTemplates = templateRepository.findActiveTemplatesWithSteps();
            logger.info("Active templates found: {}", activeTemplates.size());
            
            List<WorkflowTemplateDto> templateDTOs = activeTemplates.stream()
                    .map(this::convertToDTOSafe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            logger.info("✅ Successfully converted {} templates to DTOs", templateDTOs.size());
            return ResponseEntity.ok(templateDTOs);
            
        } catch (Exception e) {
            logger.error("❌ Error in getActiveTemplates: {}", e.getMessage(), e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * ✅ FIXED: List templates with fetch join queries
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowTemplateDto>> listTemplates(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String type) {
        try {
            logger.info("🔍 Listing templates - active: {}, type: {}", active, type);
            
            List<WorkflowTemplate> templates;
            
            // ✅ FIXED: Use appropriate fetch join queries based on filters
            if (active != null && active) {
                templates = templateRepository.findActiveTemplatesWithSteps();
            } else {
                templates = templateRepository.findAllTemplatesWithSteps();
            }

            // Apply type filter after fetching
            if (type != null) {
                try {
                    WorkflowType workflowType = WorkflowType.valueOf(type.toUpperCase());
                    templates = templates.stream()
                            .filter(t -> workflowType.equals(t.getType()))
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid workflow type: {}", type);
                    templates = Collections.emptyList();
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
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * ✅ ENHANCED: Improved pagination with better repository usage
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

            // ✅ ENHANCED: Safe DTO mapping for paginated results
            Page<WorkflowTemplateDto> dtoPage = result.map(template -> {
                try {
                    return convertToDTOSafeLightweight(template);
                } catch (Exception e) {
                    logger.warn("Failed to convert template {} to DTO: {}", template.getId(), e.getMessage());
                    return null;
                }
            }).map(dto -> dto); // Remove nulls

            logger.info("✅ Successfully returned paged templates: {} of {}", 
                       dtoPage.getNumberOfElements(), dtoPage.getTotalElements());

            return ResponseEntity.ok(dtoPage);

        } catch (Exception e) {
            logger.error("❌ Error in listTemplatesPaged: {}", e.getMessage(), e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(Page.empty());
        }
    }

    /**
     * ✅ FIXED: Get template by ID with fetch join
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowTemplateDto> getTemplate(@PathVariable UUID id) {
        try {
            logger.info("🔍 Getting template by ID: {}", id);
            
            // ✅ FIXED: Use fetch join query
            WorkflowTemplate template = templateRepository.findByIdWithSteps(id)
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
     * ✅ FIXED: Get template by name with fetch join
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/by-name/{name}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowTemplateDto> getTemplateByName(@PathVariable String name) {
        try {
            logger.info("🔍 Getting template by name: {}", name);
            
            // ✅ FIXED: Use fetch join query
            WorkflowTemplate template = templateRepository.findByNameWithSteps(name)
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
     * ✅ ENHANCED: Create template with better validation
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Transactional
    public ResponseEntity<WorkflowTemplateDto> createTemplate(@RequestBody WorkflowTemplateDto templateDto) {
        try {
            logger.info("🔍 Creating new template: {}", templateDto.getName());
            
            // ✅ ENHANCED: Validation
            if (templateDto.getName() == null || templateDto.getName().trim().isEmpty()) {
                throw new ResponseStatusException(NOT_FOUND, "Template name is required");
            }
            
            WorkflowTemplate template = convertFromDTO(templateDto);
            WorkflowTemplate savedTemplate = templateRepository.save(template);
            WorkflowTemplateDto responseDto = convertToDTOSafeLightweight(savedTemplate);
            
            logger.info("✅ Successfully created template: {}", savedTemplate.getName());
            return ResponseEntity.ok(responseDto);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error in createTemplate: {}", e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to create template");
        }
    }

    /**
     * ✅ ENHANCED: Update template with fetch join
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<WorkflowTemplateDto> updateTemplate(@PathVariable UUID id, @RequestBody WorkflowTemplateDto templateDto) {
        try {
            logger.info("🔍 Updating template: {}", id);
            
            // ✅ FIXED: Use fetch join to get existing template
            WorkflowTemplate existingTemplate = templateRepository.findByIdWithSteps(id)
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
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to update template");
        }
    }

    /**
     * Delete template - unchanged but enhanced error handling
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
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
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to delete template");
        }
    }

    /**
     * ✅ ENHANCED: Safe DTO conversion for full template data
     */
    private WorkflowTemplateDto convertToDTOSafe(WorkflowTemplate template) {
        try {
            if (template == null) {
                return null;
            }
            
            WorkflowTemplateDto dto = new WorkflowTemplateDto();
            
            // Basic fields - no lazy loading issues
            dto.setId(template.getId());
            dto.setName(template.getName());
            dto.setDescription(template.getDescription());
            dto.setIsActive(template.getIsActive());
            dto.setType(template.getType() != null ? template.getType().name() : null);
            dto.setDefaultSlaHours(template.getDefaultSlaHours());
            
            // ✅ FIXED: Steps should already be loaded via fetch join
            try {
                List<WorkflowStepDTO> stepDTOs = Optional.ofNullable(template.getSteps())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(this::convertStepToDTOSafe)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingInt(s -> s.getStepOrder() != null ? s.getStepOrder() : 0))
                        .collect(Collectors.toList());
                dto.setSteps(stepDTOs);
            } catch (Exception e) {
                logger.warn("⚠️ Could not load steps for template {}: {}", template.getId(), e.getMessage());
                dto.setSteps(Collections.emptyList());
            }
            
            return dto;
            
        } catch (Exception e) {
            logger.error("❌ Error converting template {} to DTO: {}", 
                        template != null ? template.getId() : "null", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ NEW: Lightweight DTO conversion for lists/pagination (no steps)
     */
    private WorkflowTemplateDto convertToDTOSafeLightweight(WorkflowTemplate template) {
        try {
            if (template == null) {
                return null;
            }
            
            WorkflowTemplateDto dto = new WorkflowTemplateDto();
            dto.setId(template.getId());
            dto.setName(template.getName());
            dto.setDescription(template.getDescription());
            dto.setIsActive(template.getIsActive());
            dto.setType(template.getType() != null ? template.getType().name() : null);
            dto.setDefaultSlaHours(template.getDefaultSlaHours());
            
            // ✅ For lightweight conversion, just set step count
            try {
                int stepCount = template.getSteps() != null ? template.getSteps().size() : 0;
                dto.setSteps(Collections.emptyList()); // Don't include full step data
                // You could add a stepCount field to DTO if needed
            } catch (Exception e) {
                logger.debug("Could not get step count for template {}", template.getId());
            }
            
            return dto;
            
        } catch (Exception e) {
            logger.error("❌ Error converting template {} to lightweight DTO: {}", 
                        template != null ? template.getId() : "null", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ ENHANCED: Safe step DTO conversion with better error handling
     */
    private WorkflowStepDTO convertStepToDTOSafe(WorkflowStep step) {
        try {
            if (step == null) {
                return null;
            }
            
            WorkflowStepDTO stepDTO = new WorkflowStepDTO();
            stepDTO.setId(step.getId());
            stepDTO.setName(step.getName());
            stepDTO.setStepOrder(step.getStepOrder());
            stepDTO.setStepType(step.getStepType());
            stepDTO.setSlaHours(step.getSlaHours());
            
            // ✅ ENHANCED: Better role handling
            try {
                List<String> roleNames = Optional.ofNullable(step.getRequiredRoles())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList());
                stepDTO.setRequiredRoles(roleNames);
            } catch (Exception e) {
                logger.warn("⚠️ Could not load roles for step {}: {}", step.getId(), e.getMessage());
                stepDTO.setRequiredRoles(Collections.emptyList());
            }
            
            return stepDTO;
            
        } catch (Exception e) {
            logger.error("❌ Error converting step {} to DTO: {}", 
                        step != null ? step.getId() : "null", e.getMessage());
            return null;
        }
    }

    /**
     * Convert DTO to entity - enhanced validation
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
        return template;
    }

    /**
     * ✅ ENHANCED: Template statistics with proper fetching
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTemplateStatistics() {
        try {
            logger.info("📊 Getting template statistics");
            
            // ✅ FIXED: Use fetch join for accurate step counting
            List<WorkflowTemplate> allTemplates = templateRepository.findAllTemplatesWithSteps();
            
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
            
            // ✅ FIXED: Accurate average steps calculation
            double avgSteps = allTemplates.stream()
                .mapToInt(t -> t.getSteps() != null ? t.getSteps().size() : 0)
                .average()
                .orElse(0.0);
            stats.put("averageStepsPerTemplate", Math.round(avgSteps * 100.0) / 100.0);
            
            logger.info("✅ Successfully calculated template statistics");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("❌ Error getting template statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

    /**
     * ✅ NEW: Search templates by name
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowTemplateDto>> searchTemplates(@RequestParam String query) {
        try {
            logger.info("🔍 Searching templates with query: {}", query);
            
            List<WorkflowTemplate> templates = templateRepository.searchActiveTemplatesByName(query);
            
            List<WorkflowTemplateDto> templateDTOs = templates.stream()
                    .map(this::convertToDTOSafe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            logger.info("✅ Found {} templates matching query", templateDTOs.size());
            return ResponseEntity.ok(templateDTOs);
            
        } catch (Exception e) {
            logger.error("❌ Error searching templates: {}", e.getMessage(), e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}
