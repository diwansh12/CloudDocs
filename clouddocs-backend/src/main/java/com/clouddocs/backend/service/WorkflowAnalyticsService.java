package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.analytics.MyMetricsDTO;
import com.clouddocs.backend.dto.analytics.OverviewMetricsDTO;
import com.clouddocs.backend.dto.analytics.StepMetricsDTO;
import com.clouddocs.backend.dto.analytics.TemplateMetricsDTO;
import com.clouddocs.backend.dto.analytics.projections.StepMetricsProjection;
import com.clouddocs.backend.dto.analytics.projections.TemplateCountProjection;
import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.WorkflowInstanceRepository;
import com.clouddocs.backend.repository.WorkflowTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkflowAnalyticsService {

    @Autowired private WorkflowInstanceRepository instanceRepository;
    @Autowired private WorkflowTaskRepository taskRepository;
    @Autowired private EntityManager entityManager;

    /**
     * ✅ COMPLETELY FIXED: Get overview metrics with comprehensive date filtering and logging
     */
    public OverviewMetricsDTO getOverview(LocalDateTime from, LocalDateTime to) {
        System.out.println("🔍 Analytics Debug - Date Range: " + from + " to " + to);
        
        OverviewMetricsDTO dto = new OverviewMetricsDTO();

        // ✅ CRITICAL: Force refresh to ensure fresh data from database
        entityManager.clear(); // Clear persistence context to avoid stale data
        
        List<WorkflowInstance> allWorkflows = instanceRepository.findAll();
        System.out.println("📊 Total workflows in database: " + allWorkflows.size());
        
        // ✅ ENHANCED DEBUG: Log all workflows with their dates and statuses
        allWorkflows.forEach(w -> 
            System.out.println("Workflow " + w.getId() + ": status=" + w.getStatus() + 
                ", created=" + w.getCreatedDate() + 
                ", started=" + w.getStartDate() + 
                ", ended=" + w.getEndDate())
        );

        // ✅ COMPLETELY REWRITTEN: Comprehensive date filtering logic
        List<WorkflowInstance> filteredInstances = allWorkflows.stream()
                .filter(i -> {
                    LocalDateTime dateToCheck = null;
                    
                    // ✅ CRITICAL: Use appropriate date based on workflow status
                    if (i.getStatus() == WorkflowStatus.APPROVED || i.getStatus() == WorkflowStatus.REJECTED) {
                        // For completed workflows, prefer end_date, then start_date, then created_date
                        if (i.getEndDate() != null) {
                            dateToCheck = i.getEndDate();
                        } else if (i.getStartDate() != null) {
                            dateToCheck = i.getStartDate();
                        } else {
                            dateToCheck = i.getCreatedDate();
                        }
                    } else {
                        // For pending/in-progress workflows, use start_date or created_date
                        if (i.getStartDate() != null) {
                            dateToCheck = i.getStartDate();
                        } else {
                            dateToCheck = i.getCreatedDate();
                        }
                    }
                    
                    if (dateToCheck == null) {
                        System.out.println("⚠️ Workflow " + i.getId() + " has no usable date fields");
                        return false;
                    }
                    
                    boolean inRange = !dateToCheck.isBefore(from) && !dateToCheck.isAfter(to);
                    System.out.println("📅 Workflow " + i.getId() + " (" + i.getStatus() + 
                        ") dateUsed=" + dateToCheck + " inRange=" + inRange);
                    
                    return inRange;
                })
                .collect(Collectors.toList());

        System.out.println("📊 Workflows in date range: " + filteredInstances.size());

        // ✅ ENHANCED: Count by status with detailed logging
        long approved = filteredInstances.stream()
            .filter(i -> {
                boolean isApproved = i.getStatus() == WorkflowStatus.APPROVED;
                if (isApproved) {
                    System.out.println("✅ APPROVED workflow found: ID=" + i.getId() + 
                        ", endDate=" + i.getEndDate() + ", startDate=" + i.getStartDate());
                }
                return isApproved;
            })
            .count();
        
        long rejected = filteredInstances.stream()
            .filter(i -> {
                boolean isRejected = i.getStatus() == WorkflowStatus.REJECTED;
                if (isRejected) {
                    System.out.println("❌ REJECTED workflow found: ID=" + i.getId());
                }
                return isRejected;
            })
            .count();
        
        long inProgress = filteredInstances.stream()
            .filter(i -> {
                boolean isInProgress = i.getStatus() == WorkflowStatus.IN_PROGRESS;
                if (isInProgress) {
                    System.out.println("🔄 IN_PROGRESS workflow found: ID=" + i.getId());
                }
                return isInProgress;
            })
            .count();

        long pending = filteredInstances.stream()
            .filter(i -> {
                boolean isPending = i.getStatus() == WorkflowStatus.PENDING;
                if (isPending) {
                    System.out.println("⏳ PENDING workflow found: ID=" + i.getId());
                }
                return isPending;
            })
            .count();
        
        long cancelled = filteredInstances.stream()
            .filter(i -> i.getStatus() == WorkflowStatus.CANCELLED)
            .count();

        // ✅ COMPREHENSIVE FINAL LOGGING
        System.out.println("📊 FINAL WORKFLOW COUNTS:");
        System.out.println("   ✅ Approved: " + approved);
        System.out.println("   ❌ Rejected: " + rejected);
        System.out.println("   🔄 In Progress: " + inProgress);
        System.out.println("   ⏳ Pending: " + pending);
        System.out.println("   🚫 Cancelled: " + cancelled);
        System.out.println("   📊 Total: " + (approved + rejected + inProgress + pending + cancelled));

        dto.approved = approved;
        dto.rejected = rejected;
        dto.inProgress = inProgress;
        dto.cancelled = cancelled;
        dto.total = approved + rejected + inProgress + pending + cancelled;

        // Overdue tasks (current snapshot, not filtered by date)
        try {
            dto.overdueTasks = (long) taskRepository.findOverdueTasks(LocalDateTime.now()).size();
        } catch (Exception e) {
            System.err.println("⚠️ Error getting overdue tasks: " + e.getMessage());
            dto.overdueTasks = 0L;
        }

        // Average approval hours for completed approved workflows in window
        var completedApproved = filteredInstances.stream()
                .filter(i -> i.getEndDate() != null && i.getStatus() == WorkflowStatus.APPROVED)
                .collect(Collectors.toList());
        dto.avgApprovalHours = avgDurationHours(completedApproved);

        // Average task completion from window tasks
        try {
            var tasksInPeriod = taskRepository.findTasksInDateRange(from, to);
            var completedTasks = tasksInPeriod.stream()
                    .filter(t -> t.getCompletedDate() != null)
                    .collect(Collectors.toList());
            dto.avgTaskCompletionHours = avgTaskDurationHours(completedTasks);

            // Additional metrics
            dto.totalTasksInPeriod = (long) tasksInPeriod.size();
            dto.completedTasksInPeriod = (long) completedTasks.size();
            dto.completionRate = tasksInPeriod.isEmpty() ? null : 
                round2((double) completedTasks.size() / tasksInPeriod.size() * 100);
        } catch (Exception e) {
            System.err.println("⚠️ Error calculating task metrics: " + e.getMessage());
            dto.avgTaskCompletionHours = null;
            dto.totalTasksInPeriod = 0L;
            dto.completedTasksInPeriod = 0L;
            dto.completionRate = null;
        }

        System.out.println("📊 Final OverviewDTO - Total: " + dto.total + ", Approved: " + dto.approved);
        return dto;
    }

    /**
     * ✅ ENHANCED: Get template metrics with better error handling and logging
     */
    public List<TemplateMetricsDTO> getByTemplate(LocalDateTime from, LocalDateTime to) {
        System.out.println("🔍 Template Metrics - Date Range: " + from + " to " + to);
        
        try {
            var projections = instanceRepository.aggregateByTemplateBetween(from, to);
            List<TemplateMetricsDTO> results = new ArrayList<>();
            
            System.out.println("📊 Template projections found: " + projections.size());
            
            for (TemplateCountProjection p : projections) {
                TemplateMetricsDTO dto = new TemplateMetricsDTO();
                dto.templateId = p.getTemplateId();
                dto.templateName = p.getTemplateName();
                dto.total = p.getTotal();
                dto.approved = p.getApproved();
                dto.rejected = p.getRejected();
                dto.avgDurationHours = round2OrNull(p.getAvgDurationHours());
                dto.approvalRate = dto.total > 0L ? 
                    round2((double) dto.approved / dto.total * 100) : null;
                results.add(dto);
                
                System.out.println("📋 Template " + dto.templateName + ": total=" + dto.total + 
                    ", approved=" + dto.approved + ", rejected=" + dto.rejected);
            }
            return results;
        } catch (Exception e) {
            System.err.println("⚠️ Template projection failed, using manual aggregation: " + e.getMessage());
            return aggregateTemplateMetricsManually(from, to);
        }
    }

    /**
     * ✅ ENHANCED: Manual template metrics aggregation with better filtering
     */
    private List<TemplateMetricsDTO> aggregateTemplateMetricsManually(LocalDateTime from, LocalDateTime to) {
        System.out.println("🔄 Manual template aggregation - Date Range: " + from + " to " + to);
        
        List<WorkflowInstance> allWorkflows = instanceRepository.findAll();
        List<WorkflowInstance> filteredInstances = allWorkflows.stream()
                .filter(i -> {
                    LocalDateTime dateToCheck = null;
                    
                    // Use same date logic as overview
                    if (i.getStatus() == WorkflowStatus.APPROVED || i.getStatus() == WorkflowStatus.REJECTED) {
                        dateToCheck = i.getEndDate() != null ? i.getEndDate() : 
                                     (i.getStartDate() != null ? i.getStartDate() : i.getCreatedDate());
                    } else {
                        dateToCheck = i.getStartDate() != null ? i.getStartDate() : i.getCreatedDate();
                    }
                    
                    return dateToCheck != null && !dateToCheck.isBefore(from) && !dateToCheck.isAfter(to);
                })
                .collect(Collectors.toList());

        System.out.println("📊 Filtered workflows for templates: " + filteredInstances.size());

        Map<String, TemplateMetricsDTO> templateMap = new HashMap<>();
        
        for (WorkflowInstance instance : filteredInstances) {
            String templateId = instance.getTemplate() != null ? 
                instance.getTemplate().getId().toString() : "unknown";
            String templateName = instance.getTemplate() != null ? 
                instance.getTemplate().getName() : "Unknown Template";
            
            TemplateMetricsDTO dto = templateMap.computeIfAbsent(templateId, k -> {
                TemplateMetricsDTO newDto = new TemplateMetricsDTO();
                newDto.templateId = templateId;
                newDto.templateName = templateName;
                newDto.total = 0L;
                newDto.approved = 0L;
                newDto.rejected = 0L;
                return newDto;
            });
            
            dto.total++;
            if (instance.getStatus() == WorkflowStatus.APPROVED) {
                dto.approved++;
            } else if (instance.getStatus() == WorkflowStatus.REJECTED) {
                dto.rejected++;
            }
            
            dto.approvalRate = dto.total > 0L ? round2((double) dto.approved / dto.total * 100) : null;
        }
        
        List<TemplateMetricsDTO> results = new ArrayList<>(templateMap.values());
        System.out.println("📋 Manual template metrics generated: " + results.size() + " templates");
        
        return results;
    }

    /**
     * ✅ ENHANCED: Get step metrics with better error handling
     */
    public List<StepMetricsDTO> getByStep(LocalDateTime from, LocalDateTime to) {
        System.out.println("🔍 Step Metrics - Date Range: " + from + " to " + to);
        
        try {
            var projections = taskRepository.aggregateByStepBetween(from, to);
            List<StepMetricsDTO> results = new ArrayList<>();
            
            System.out.println("📊 Step projections found: " + projections.size());
            
            for (StepMetricsProjection p : projections) {
                StepMetricsDTO dto = new StepMetricsDTO();
                dto.stepOrder = p.getStepOrder();
                dto.avgTaskCompletionHours = round2OrNull(p.getAvgTaskCompletionHours());
                dto.approvals = p.getApprovals();
                dto.rejections = p.getRejections();
                dto.totalTasks = p.getTotalTasks();
                dto.completedTasks = p.getCompletedTasks();
                dto.pendingTasks = p.getPendingTasks();
                dto.overdueTasks = p.getOverdueTasks();
                dto.completionRate = dto.totalTasks > 0L ? 
                    round2((double) dto.completedTasks / dto.totalTasks * 100) : null;
                results.add(dto);
                
                System.out.println("🔢 Step " + dto.stepOrder + ": total=" + dto.totalTasks + 
                    ", completed=" + dto.completedTasks + ", approvals=" + dto.approvals);
            }
            
            results.sort(Comparator.comparingInt(a -> a.stepOrder != null ? a.stepOrder : 0));
            return results;
        } catch (Exception e) {
            System.err.println("❌ Error getting step metrics: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * ✅ ENHANCED: Get user's personal metrics with comprehensive filtering
     */
    public MyMetricsDTO getMyMetrics(User currentUser, LocalDateTime from, LocalDateTime to) {
        System.out.println("🔍 My Metrics for user " + currentUser.getId() + " - Date Range: " + from + " to " + to);
        
        MyMetricsDTO dto = new MyMetricsDTO();

        // Filter user's initiated workflows by date using same logic as overview
        var allWorkflows = instanceRepository.findAll();
        var myWorkflows = allWorkflows.stream()
                .filter(i -> {
                    // Check if user initiated this workflow
                    boolean isMyWorkflow = i.getInitiatedBy() != null && 
                        i.getInitiatedBy().getId().equals(currentUser.getId());
                    
                    if (!isMyWorkflow) return false;
                    
                    // Use same date filtering logic as overview
                    LocalDateTime dateToCheck = null;
                    if (i.getStatus() == WorkflowStatus.APPROVED || i.getStatus() == WorkflowStatus.REJECTED) {
                        dateToCheck = i.getEndDate() != null ? i.getEndDate() : 
                                     (i.getStartDate() != null ? i.getStartDate() : i.getCreatedDate());
                    } else {
                        dateToCheck = i.getStartDate() != null ? i.getStartDate() : i.getCreatedDate();
                    }
                    
                    return dateToCheck != null && !dateToCheck.isBefore(from) && !dateToCheck.isAfter(to);
                })
                .collect(Collectors.toList());

        System.out.println("📊 My workflows in range: " + myWorkflows.size());

        dto.myInitiatedTotal = (long) myWorkflows.size();
        dto.myInitiatedApproved = myWorkflows.stream()
            .filter(i -> i.getStatus() == WorkflowStatus.APPROVED)
            .count();
        dto.myInitiatedRejected = myWorkflows.stream()
            .filter(i -> i.getStatus() == WorkflowStatus.REJECTED)
            .count();

        System.out.println("📊 My workflow counts - Total: " + dto.myInitiatedTotal + 
            ", Approved: " + dto.myInitiatedApproved + ", Rejected: " + dto.myInitiatedRejected);

        // User's tasks in date range
        try {
            var allTasks = taskRepository.findTasksInDateRange(from, to);
            var myTasks = allTasks.stream()
                    .filter(t -> t.getAssignedTo() != null && 
                               t.getAssignedTo().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());

            dto.myPendingTasks = taskRepository.countByAssignedToAndStatus(currentUser, TaskStatus.PENDING);
            dto.myCompletedTasks = myTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

            var completedTasks = myTasks.stream()
                .filter(t -> t.getCompletedDate() != null)
                .collect(Collectors.toList());
            dto.myAvgTaskCompletionHours = avgTaskDurationHours(completedTasks);

            dto.myTaskCompletionRate = myTasks.isEmpty() ? null :
                round2((double) completedTasks.size() / myTasks.size() * 100);
                
            System.out.println("📊 My task counts - Pending: " + dto.myPendingTasks + 
                ", Completed: " + dto.myCompletedTasks);
        } catch (Exception e) {
            System.err.println("⚠️ Error calculating my task metrics: " + e.getMessage());
            dto.myPendingTasks = 0L;
            dto.myCompletedTasks = 0L;
            dto.myAvgTaskCompletionHours = null;
            dto.myTaskCompletionRate = null;
        }

        return dto;
    }

    /**
     * Export overview metrics as CSV
     */
    public byte[] exportOverviewCsv(LocalDateTime from, LocalDateTime to) {
        try {
            var overview = getOverview(from, to);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append(String.format("Total Workflows,%d\n", overview.total));
            csv.append(String.format("Approved,%d\n", overview.approved));
            csv.append(String.format("Rejected,%d\n", overview.rejected));
            csv.append(String.format("In Progress,%d\n", overview.inProgress));
            csv.append(String.format("Cancelled,%d\n", overview.cancelled));
            csv.append(String.format("Overdue Tasks,%d\n", overview.overdueTasks));
            csv.append(String.format("Avg Approval Hours,%s\n", 
                overview.avgApprovalHours != null ? overview.avgApprovalHours.toString() : "N/A"));
            csv.append(String.format("Avg Task Completion Hours,%s\n", 
                overview.avgTaskCompletionHours != null ? overview.avgTaskCompletionHours.toString() : "N/A"));
            csv.append(String.format("Completion Rate (%%),%s\n", 
                overview.completionRate != null ? overview.completionRate.toString() : "N/A"));
            
            return csv.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            System.err.println("❌ Overview CSV export failed: " + e.getMessage());
            throw new RuntimeException("Failed to generate overview CSV", e);
        }
    }

    /**
     * Export template metrics as CSV
     */
    public byte[] exportTemplateMetricsCsv(LocalDateTime from, LocalDateTime to) {
        try {
            var metrics = getByTemplate(from, to);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Template ID,Template Name,Total,Approved,Rejected,Approval Rate (%),Avg Duration (Hours)\n");
            
            for (var metric : metrics) {
                csv.append(String.format("%s,%s,%d,%d,%d,%s,%s\n",
                    metric.templateId,
                    escapeCsv(metric.templateName),
                    metric.total,
                    metric.approved,
                    metric.rejected,
                    metric.approvalRate != null ? metric.approvalRate.toString() : "N/A",
                    metric.avgDurationHours != null ? metric.avgDurationHours.toString() : "N/A"
                ));
            }
            
            return csv.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            System.err.println("❌ Template CSV export failed: " + e.getMessage());
            throw new RuntimeException("Failed to generate template metrics CSV", e);
        }
    }

    /**
     * Export step metrics as CSV
     */
    public byte[] exportStepMetricsCsv(LocalDateTime from, LocalDateTime to) {
        try {
            var metrics = getByStep(from, to);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Step Order,Avg Completion Hours,Total Tasks,Completed Tasks,Approvals,Rejections,Completion Rate (%)\n");
            
            for (var metric : metrics) {
                csv.append(String.format("%s,%s,%d,%d,%d,%d,%s\n",
                    metric.stepOrder != null ? metric.stepOrder.toString() : "",
                    metric.avgTaskCompletionHours != null ? metric.avgTaskCompletionHours.toString() : "",
                    metric.totalTasks,
                    metric.completedTasks,
                    metric.approvals,
                    metric.rejections,
                    metric.completionRate != null ? metric.completionRate.toString() : "N/A"
                ));
            }
            
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("❌ Step CSV export failed: " + e.getMessage());
            throw new RuntimeException("Failed to generate step metrics CSV", e);
        }
    }

    // ===== Helper Methods =====

    private Double avgDurationHours(List<WorkflowInstance> completed) {
        if (completed == null || completed.isEmpty()) return null;
        double sum = 0;
        int count = 0;
        for (WorkflowInstance i : completed) {
            if (i.getStartDate() != null && i.getEndDate() != null) {
                sum += Duration.between(i.getStartDate(), i.getEndDate()).toMinutes() / 60.0;
                count++;
            }
        }
        return count == 0 ? null : round2(sum / count);
    }

    private Double avgTaskDurationHours(List<WorkflowTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return null;
        double sum = 0;
        int count = 0;
        for (WorkflowTask t : tasks) {
            if (t.getCreatedDate() != null && t.getCompletedDate() != null) {
                sum += Duration.between(t.getCreatedDate(), t.getCompletedDate()).toMinutes() / 60.0;
                count++;
            }
        }
        return count == 0 ? null : round2(sum / count);
    }

    private Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private Double round2OrNull(Double v) {
        return v == null ? null : round2(v);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String escaped = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
