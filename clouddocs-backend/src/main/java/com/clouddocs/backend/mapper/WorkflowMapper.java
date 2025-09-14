package com.clouddocs.backend.mapper;

import com.clouddocs.backend.dto.workflow.WorkflowHistoryDTO;
import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.dto.workflow.WorkflowStepDTO;
import com.clouddocs.backend.dto.workflow.WorkflowTaskDTO;
import com.clouddocs.backend.entity.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ✅ FIXED: Production-ready utility class for mapping workflow entities to DTOs
 * Includes comprehensive null checks, error handling, and enhanced data mapping
 */
@Slf4j
public class WorkflowMapper {

    private WorkflowMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * ✅ ENHANCED: Convert WorkflowInstance entity to DTO with comprehensive mapping
     */
    public static WorkflowInstanceDTO toInstanceDTO(WorkflowInstance instance) {
        if (instance == null) {
            return null;
        }

        try {
            WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
            
            // ✅ Basic fields with null safety
            dto.setId(instance.getId());
            dto.setStatus(instance.getStatus());
            dto.setCurrentStepOrder(instance.getCurrentStepOrder());
            dto.setPriority(instance.getPriority());
            dto.setComments(instance.getComments());
            
            // ✅ Enhanced: Add title and description mapping
            dto.setTitle(instance.getTitle() != null ? instance.getTitle() : "Workflow");
            dto.setDescription(instance.getDescription());
            
            // ✅ FIXED: Date fields - no conversion needed since both use OffsetDateTime
            dto.setStartDate(instance.getStartDate());
            dto.setEndDate(instance.getEndDate());
            dto.setDueDate(instance.getDueDate());
            dto.setCreatedDate(instance.getCreatedDate());

            // ✅ Fix for "Last Updated stuck"
            if (instance.getUpdatedDate() != null) {
                dto.setUpdatedDate(instance.getUpdatedDate());
            } else {
                dto.setUpdatedDate(instance.getCreatedDate());
            }
            
            // ✅ Enhanced: Template information
            mapTemplateInfo(instance, dto);
            
            // ✅ Enhanced: Document details with fallback
            mapDocumentInfo(instance, dto);

            // ✅ Enhanced: Initiator details with fallback
            mapInitiatorInfo(instance, dto);

            // ✅ Enhanced: Task summary calculations
            mapTaskSummary(instance, dto);

            // ✅ Map related collections safely
            dto.setTasks(mapTasks(instance.getTasks()));
            dto.setHistory(mapHistory(instance.getHistory()));
            dto.setSteps(mapSteps(instance.getTemplate()));

            return dto;
            
        } catch (Exception e) {
            log.error("Error mapping WorkflowInstance to DTO: {}", e.getMessage(), e);
            return createMinimalInstanceDTO(instance);
        }
    }

    /**
     * ✅ ENHANCED: Map template information safely
     */
    private static void mapTemplateInfo(WorkflowInstance instance, WorkflowInstanceDTO dto) {
        try {
            if (instance.getTemplate() != null) {
                dto.setTemplateId(instance.getTemplate().getId());
                dto.setTemplateName(instance.getTemplate().getName() != null ? 
                    instance.getTemplate().getName() : "Unknown Template");
            }
        } catch (Exception e) {
            log.debug("Could not map template info: {}", e.getMessage());
            dto.setTemplateName("Template Load Error");
        }
    }

    /**
     * ✅ ENHANCED: Map document information with fallbacks
     */
    private static void mapDocumentInfo(WorkflowInstance instance, WorkflowInstanceDTO dto) {
        try {
            if (instance.getDocument() != null) {
                dto.setDocumentId(instance.getDocument().getId());
                
                // Enhanced document name handling
                String documentName = instance.getDocument().getOriginalFilename();
                if (documentName == null || documentName.trim().isEmpty()) {
                    documentName = instance.getDocument().getFilename();
                }
                if (documentName == null || documentName.trim().isEmpty()) {
                    documentName = "Unknown Document";
                }
                dto.setDocumentName(documentName);
            } else {
                dto.setDocumentName("No Document");
            }
        } catch (Exception e) {
            log.debug("Could not map document info: {}", e.getMessage());
            dto.setDocumentName("Document Load Error");
        }
    }

    /**
     * ✅ ENHANCED: Map initiator information with fallbacks
     */
    private static void mapInitiatorInfo(WorkflowInstance instance, WorkflowInstanceDTO dto) {
        try {
            if (instance.getInitiatedBy() != null) {
                dto.setInitiatedById(instance.getInitiatedBy().getId());
                
                // Enhanced name handling with fallbacks
                String initiatedByName = instance.getInitiatedBy().getFullName();
                if (initiatedByName == null || initiatedByName.trim().isEmpty()) {
                    initiatedByName = instance.getInitiatedBy().getUsername();
                }
                if (initiatedByName == null || initiatedByName.trim().isEmpty()) {
                    initiatedByName = "User ID: " + instance.getInitiatedBy().getId();
                }
                dto.setInitiatedByName(initiatedByName);
            } else {
                dto.setInitiatedByName("System");
            }
        } catch (Exception e) {
            log.debug("Could not map initiator info: {}", e.getMessage());
            dto.setInitiatedByName("Unknown User");
        }
    }

    /**
     * ✅ ENHANCED: Calculate and map task summary information
     */
    private static void mapTaskSummary(WorkflowInstance instance, WorkflowInstanceDTO dto) {
        try {
            if (instance.getTasks() != null && !instance.getTasks().isEmpty()) {
                int totalTasks = instance.getTasks().size();
                long completedTasks = instance.getTasks().stream()
                        .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                        .count();
                
                dto.setTotalTasks(totalTasks);
                dto.setCompletedTasks((int) completedTasks);
            } else {
                dto.setTotalTasks(0);
                dto.setCompletedTasks(0);
            }
        } catch (Exception e) {
            log.debug("Could not calculate task summary: {}", e.getMessage());
            dto.setTotalTasks(0);
            dto.setCompletedTasks(0);
        }
    }

    /**
     * ✅ FIXED: Map workflow tasks to DTOs
     */
    private static List<WorkflowTaskDTO> mapTasks(List<WorkflowTask> tasks) {
        List<WorkflowTaskDTO> taskDTOs = new ArrayList<>();
        
        if (tasks == null) {
            return taskDTOs;
        }

        for (WorkflowTask task : tasks) {
            try {
                WorkflowTaskDTO taskDTO = new WorkflowTaskDTO();
                
                // Basic task information
                taskDTO.setId(task.getId());
                taskDTO.setTitle(task.getTitle() != null ? task.getTitle() : "Task");
                taskDTO.setDescription(task.getDescription());
                taskDTO.setStatus(task.getStatus());
                taskDTO.setAction(task.getAction());
                taskDTO.setPriority(task.getPriority());
                
                // ✅ FIXED: Date information
                taskDTO.setCreatedDate(task.getCreatedDate());
                taskDTO.setDueDate(task.getDueDate());
                taskDTO.setCompletedDate(task.getCompletedDate());

                // ✅ Enhanced: Assignee details with fallback
                if (task.getAssignedTo() != null) {
                    taskDTO.setAssignedToId(task.getAssignedTo().getId());
                    String assigneeName = task.getAssignedTo().getFullName();
                    if (assigneeName == null || assigneeName.trim().isEmpty()) {
                        assigneeName = task.getAssignedTo().getUsername();
                    }
                    taskDTO.setAssignedToName(assigneeName != null ? assigneeName : "Unknown User");
                } else {
                    taskDTO.setAssignedToName("Unassigned");
                }

                // ✅ Enhanced: Step details with fallback
                if (task.getWorkflowStep() != null) {
                    taskDTO.setStepOrder(task.getWorkflowStep().getStepOrder());
                    taskDTO.setStepName(task.getWorkflowStep().getName() != null ? 
                        task.getWorkflowStep().getName() : "Step " + task.getWorkflowStep().getStepOrder());
                } else {
                    taskDTO.setStepName("Unknown Step");
                    taskDTO.setStepOrder(0);
                }

                taskDTOs.add(taskDTO);
                
            } catch (Exception e) {
                log.warn("Error mapping task {}: {}", task != null ? task.getId() : "null", e.getMessage());
                // Continue with other tasks instead of failing completely
            }
        }

        // ✅ Sort by created date (newest first)
        taskDTOs.sort((a, b) -> {
            if (a.getCreatedDate() == null) return 1;
            if (b.getCreatedDate() == null) return -1;
            return b.getCreatedDate().compareTo(a.getCreatedDate());
        });

        return taskDTOs;
    }

    /**
     * ✅ FIXED: Map workflow history to DTOs
     */
    private static List<WorkflowHistoryDTO> mapHistory(List<WorkflowHistory> historyList) {
        List<WorkflowHistoryDTO> historyDTOs = new ArrayList<>();
        
        if (historyList == null) {
            return historyDTOs;
        }

        for (WorkflowHistory history : historyList) {
            try {
                WorkflowHistoryDTO historyDTO = new WorkflowHistoryDTO();
                historyDTO.setId(history.getId());
                historyDTO.setActionDate(history.getActionDate());
                historyDTO.setDetails(history.getDetails());
                historyDTO.setAction(history.getAction());

                // ✅ Fixed: Correct method name for performer details
                if (history.getPerformedBy() != null) {
                    historyDTO.setPerformedById(history.getPerformedBy().getId());
                    String performerName = history.getPerformedBy().getFullName();
                    if (performerName == null || performerName.trim().isEmpty()) {
                        performerName = history.getPerformedBy().getUsername();
                    }
                    historyDTO.setPerformedByName(performerName != null ? performerName : "Unknown User");
                } else {
                    historyDTO.setPerformedByName("System");
                }

                historyDTOs.add(historyDTO);
                
            } catch (Exception e) {
                log.warn("Error mapping history {}: {}", history != null ? history.getId() : "null", e.getMessage());
                // Continue with other history entries
            }
        }

        // ✅ Sort by action date (chronological order)
        historyDTOs.sort((a, b) -> {
            if (a.getActionDate() == null) return 1;
            if (b.getActionDate() == null) return -1;
            return a.getActionDate().compareTo(b.getActionDate());
        });

        return historyDTOs;
    }

   /**
 * ✅ FIXED: Map workflow template steps to DTOs - ERole compilation errors resolved
 */
private static List<WorkflowStepDTO> mapSteps(WorkflowTemplate template) {
    List<WorkflowStepDTO> stepDTOs = new ArrayList<>();
    
    if (template == null || template.getSteps() == null) {
        return stepDTOs;
    }

    for (WorkflowStep step : template.getSteps()) {
        try {
            WorkflowStepDTO stepDTO = new WorkflowStepDTO();
            stepDTO.setId(step.getId());
            stepDTO.setStepOrder(step.getStepOrder());
            stepDTO.setName(step.getName() != null ? step.getName() : "Step " + step.getStepOrder());
            stepDTO.setStepType(step.getStepType());
            stepDTO.setSlaHours(step.getSlaHours());

            // ✅ FIXED: Correct ERole enum mapping
            List<String> requiredRoles = new ArrayList<>();
            try {
                if (step.getRoles() != null && !step.getRoles().isEmpty()) {
                    requiredRoles = step.getRoles().stream()
                        .filter(Objects::nonNull)
                        .filter(stepRole -> stepRole.getRoleName() != null)
                        .map(stepRole -> stepRole.getRoleName().name()) // ✅ FIXED: ERole.name() not getName().name()
                        .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.debug("Could not map required roles for step {}: {}", step.getId(), e.getMessage());
                requiredRoles = new ArrayList<>();
            }
            
            stepDTO.setRequiredRoles(requiredRoles);
            stepDTOs.add(stepDTO);
            
        } catch (Exception e) {
            log.warn("Error mapping step {}: {}", step != null ? step.getId() : "null", e.getMessage());
        }
    }

    // Sort by step order
    stepDTOs.sort((a, b) -> {
        if (a.getStepOrder() == null) return 1;
        if (b.getStepOrder() == null) return -1;
        return a.getStepOrder().compareTo(b.getStepOrder());
    });

    return stepDTOs;
}

    /**
     * ✅ ENHANCED: Create minimal DTO when full mapping fails
     */
    private static WorkflowInstanceDTO createMinimalInstanceDTO(WorkflowInstance instance) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        try {
            dto.setId(instance.getId());
            dto.setStatus(instance.getStatus());
            dto.setTitle(instance.getTitle() != null ? instance.getTitle() : "Workflow");
            dto.setTotalTasks(0);
            dto.setCompletedTasks(0);
            dto.setTasks(new ArrayList<>());
            dto.setHistory(new ArrayList<>());
            dto.setSteps(new ArrayList<>());
        } catch (Exception e) {
            log.error("Failed to create even minimal DTO: {}", e.getMessage());
        }
        return dto;
    }

    /**
     * ✅ FIXED: Helper method to safely map a single WorkflowTask to DTO
     */
    public static WorkflowTaskDTO toTaskDTO(WorkflowTask task) {
        if (task == null) {
            return null;
        }

        try {
            WorkflowTaskDTO dto = new WorkflowTaskDTO();
            dto.setId(task.getId());
            dto.setTitle(task.getTitle() != null ? task.getTitle() : "Task");
            dto.setDescription(task.getDescription());
            dto.setStatus(task.getStatus());
            dto.setAction(task.getAction());
            dto.setPriority(task.getPriority());
            dto.setCreatedDate(task.getCreatedDate());
            dto.setDueDate(task.getDueDate());
            dto.setCompletedDate(task.getCompletedDate());

            // Assignee information
            if (task.getAssignedTo() != null) {
                dto.setAssignedToId(task.getAssignedTo().getId());
                String assigneeName = task.getAssignedTo().getFullName();
                if (assigneeName == null || assigneeName.trim().isEmpty()) {
                    assigneeName = task.getAssignedTo().getUsername();
                }
                dto.setAssignedToName(assigneeName != null ? assigneeName : "Unknown User");
            }

            // Step information
            if (task.getWorkflowStep() != null) {
                dto.setStepOrder(task.getWorkflowStep().getStepOrder());
                dto.setStepName(task.getWorkflowStep().getName() != null ? 
                    task.getWorkflowStep().getName() : "Step " + task.getWorkflowStep().getStepOrder());
            }

            return dto;

        } catch (Exception e) {
            log.error("Error mapping WorkflowTask to DTO: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
 * ✅ FIXED: Helper method to safely map a single WorkflowStep to DTO
 */
public static WorkflowStepDTO toStepDTO(WorkflowStep step) {
    if (step == null) {
        return null;
    }

    try {
        WorkflowStepDTO dto = new WorkflowStepDTO();
        dto.setId(step.getId());
        dto.setStepOrder(step.getStepOrder());
        dto.setName(step.getName() != null ? step.getName() : "Step " + step.getStepOrder());
        dto.setStepType(step.getStepType());
        dto.setSlaHours(step.getSlaHours());

        // ✅ FIXED: Correct ERole enum mapping
        List<String> requiredRoles = new ArrayList<>();
        try {
            if (step.getRoles() != null && !step.getRoles().isEmpty()) {
                requiredRoles = step.getRoles().stream()
                    .filter(Objects::nonNull)
                    .filter(stepRole -> stepRole.getRoleName() != null)
                    .map(stepRole -> stepRole.getRoleName().name()) // ✅ FIXED: ERole.name() directly
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("Could not map required roles for step {}: {}", step.getId(), e.getMessage());
        }

        dto.setRequiredRoles(requiredRoles);
        return dto;

    } catch (Exception e) {
        log.error("Error mapping WorkflowStep to DTO: {}", e.getMessage(), e);
        return null;
    }
}
}
