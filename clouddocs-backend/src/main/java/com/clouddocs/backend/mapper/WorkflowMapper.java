package com.clouddocs.backend.mapper;

import com.clouddocs.backend.dto.workflow.WorkflowHistoryDTO;
import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.dto.workflow.WorkflowStepDTO;
import com.clouddocs.backend.dto.workflow.WorkflowTaskDTO;
import com.clouddocs.backend.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping workflow entities to DTOs
 */
public class WorkflowMapper {

    private WorkflowMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert WorkflowInstance entity to DTO with all related data
     */
    public static WorkflowInstanceDTO toInstanceDTO(WorkflowInstance instance) {
    if (instance == null) {
        return null;
    }

    WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
    
    // Basic fields
    dto.setId(instance.getId());
    dto.setStatus(instance.getStatus());
    dto.setCurrentStepOrder(instance.getCurrentStepOrder());
    dto.setStartDate(instance.getStartDate());
    dto.setEndDate(instance.getEndDate());
    dto.setDueDate(instance.getDueDate());
    dto.setPriority(instance.getPriority());
    dto.setComments(instance.getComments());
    
    // ✅ ADD THIS MISSING LINE
    dto.setUpdatedDate(instance.getUpdatedDate());

    // Document details
    if (instance.getDocument() != null) {
        dto.setDocumentId(instance.getDocument().getId());
        // Use filename if originalFilename is null
        String documentName = instance.getDocument().getOriginalFilename() != null ? 
            instance.getDocument().getOriginalFilename() : 
            instance.getDocument().getFilename();
        dto.setDocumentName(documentName);
    }

    // Initiator details
    if (instance.getInitiatedBy() != null) {
        dto.setInitiatedById(instance.getInitiatedBy().getId());
        // Fallback to username if fullName is null
        String initiatedByName = instance.getInitiatedBy().getFullName() != null ? 
            instance.getInitiatedBy().getFullName() : 
            instance.getInitiatedBy().getUsername();
        dto.setInitiatedByName(initiatedByName);
    }

    // Map tasks, history, steps
    dto.setTasks(mapTasks(instance.getTasks()));
    dto.setHistory(mapHistory(instance.getHistory()));
    dto.setSteps(mapSteps(instance.getTemplate()));
dto.setCreatedDate(instance.getCreatedDate());
dto.setUpdatedDate(instance.getUpdatedDate());
    return dto;
}
    /**
     * Map workflow tasks to DTOs
     */
    private static List<WorkflowTaskDTO> mapTasks(List<WorkflowTask> tasks) {
        List<WorkflowTaskDTO> taskDTOs = new ArrayList<>();
        
        if (tasks != null) {
            for (WorkflowTask task : tasks) {
                WorkflowTaskDTO taskDTO = new WorkflowTaskDTO();
                taskDTO.setId(task.getId());
                taskDTO.setTitle(task.getTitle());
                taskDTO.setDescription(task.getDescription());
                taskDTO.setStatus(task.getStatus());
                taskDTO.setAction(task.getAction());
                taskDTO.setPriority(task.getPriority());
                taskDTO.setCreatedDate(task.getCreatedDate());
                taskDTO.setDueDate(task.getDueDate());
                taskDTO.setCompletedDate(task.getCompletedDate());

                // Assignee details
                if (task.getAssignedTo() != null) {
                    taskDTO.setAssignedToId(task.getAssignedTo().getId());
                    taskDTO.setAssignedToName(task.getAssignedTo().getFullName());
                }

                // Step details
                if (task.getWorkflowStep() != null) {
                    taskDTO.setStepOrder(task.getWorkflowStep().getStepOrder());
                    taskDTO.setStepName(task.getWorkflowStep().getName());
                }

                taskDTOs.add(taskDTO);
            }

            // Sort by created date (newest first)
            taskDTOs.sort((a, b) -> {
                if (a.getCreatedDate() == null) return 1;
                if (b.getCreatedDate() == null) return -1;
                return b.getCreatedDate().compareTo(a.getCreatedDate());
            });
        }

        return taskDTOs;
    }

    /**
     * Map workflow history to DTOs
     */
    private static List<WorkflowHistoryDTO> mapHistory(List<WorkflowHistory> historyList) {
        List<WorkflowHistoryDTO> historyDTOs = new ArrayList<>();
        
        if (historyList != null) {
            for (WorkflowHistory history : historyList) {
                WorkflowHistoryDTO historyDTO = new WorkflowHistoryDTO();
                historyDTO.setId(history.getId());
                historyDTO.setActionDate(history.getActionDate());
                historyDTO.setDetails(history.getDetails());
                historyDTO.setAction(history.getAction());

                // Performer details
                if (history.getPerformedBy() != null) {
                    historyDTO.setPerformedById(history.getPerformedBy().getId());
                    historyDTO.setPerformedByName(history.getPerformedBy().getFullName());
                }

                historyDTOs.add(historyDTO);
            }

            // Sort by action date (oldest first for chronological order)
            historyDTOs.sort((a, b) -> {
                if (a.getActionDate() == null) return 1;
                if (b.getActionDate() == null) return -1;
                return a.getActionDate().compareTo(b.getActionDate());
            });
        }

        return historyDTOs;
    }

    /**
     * Map workflow template steps to DTOs
     */
    private static List<WorkflowStepDTO> mapSteps(WorkflowTemplate template) {
        List<WorkflowStepDTO> stepDTOs = new ArrayList<>();
        
        if (template != null && template.getSteps() != null) {
            for (WorkflowStep step : template.getSteps()) {
                WorkflowStepDTO stepDTO = new WorkflowStepDTO();
                stepDTO.setStepOrder(step.getStepOrder());
                stepDTO.setName(step.getName());
                stepDTO.setStepType(step.getStepType());
                stepDTO.setSlaHours(step.getSlaHours());

                // Map required roles
                if (step.getRequiredRoles() != null) {
                    stepDTO.setRequiredRoles(
                        step.getRequiredRoles().stream()
                            .map(Enum::name)
                            .collect(Collectors.toList())
                    );
                }

                stepDTOs.add(stepDTO);
            }

            // Sort by step order
            stepDTOs.sort((a, b) -> {
                if (a.getStepOrder() == null) return 1;
                if (b.getStepOrder() == null) return -1;
                return a.getStepOrder().compareTo(b.getStepOrder());
            });
        }

        return stepDTOs;
    }
}
