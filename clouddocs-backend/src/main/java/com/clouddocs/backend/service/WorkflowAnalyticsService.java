package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.analytics.MyMetricsDTO;
import com.clouddocs.backend.dto.analytics.OverviewMetricsDTO;
import com.clouddocs.backend.dto.analytics.StepMetricsDTO;
import com.clouddocs.backend.dto.analytics.TemplateMetricsDTO;
import com.clouddocs.backend.entity.*;
import com.clouddocs.backend.repository.WorkflowInstanceRepository;
import com.clouddocs.backend.repository.WorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class WorkflowAnalyticsService {

    @Autowired private WorkflowInstanceRepository instanceRepository;
    @Autowired private WorkflowTaskRepository taskRepository;
    @Autowired private EntityManager entityManager;

    /**
     * ✅ COMPLETELY FIXED: Direct database queries for accurate analytics
     */
    public OverviewMetricsDTO getOverview(LocalDateTime from, LocalDateTime to) {
        log.info("🔍 Getting overview analytics - Date Range: {} to {}", from, to);
        
        OverviewMetricsDTO dto = new OverviewMetricsDTO();

        try {
            // ✅ CRITICAL FIX: Use direct database queries instead of loading all data
            
            // Total workflows in date range
            Query totalQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances " +
                "WHERE COALESCE(end_date, start_date, created_date) BETWEEN :from AND :to"
            );
            totalQuery.setParameter("from", from);
            totalQuery.setParameter("to", to);
            dto.total = ((Number) totalQuery.getSingleResult()).longValue();
            
            // Approved workflows
            Query approvedQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances " +
                "WHERE status = 'APPROVED' " +
                "AND COALESCE(end_date, start_date, created_date) BETWEEN :from AND :to"
            );
            approvedQuery.setParameter("from", from);
            approvedQuery.setParameter("to", to);
            dto.approved = ((Number) approvedQuery.getSingleResult()).longValue();
            
            // Rejected workflows
            Query rejectedQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances " +
                "WHERE status = 'REJECTED' " +
                "AND COALESCE(end_date, start_date, created_date) BETWEEN :from AND :to"
            );
            rejectedQuery.setParameter("from", from);
            rejectedQuery.setParameter("to", to);
            dto.rejected = ((Number) rejectedQuery.getSingleResult()).longValue();
            
            // In Progress workflows
            Query inProgressQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances " +
                "WHERE status = 'IN_PROGRESS' " +
                "AND COALESCE(start_date, created_date) BETWEEN :from AND :to"
            );
            inProgressQuery.setParameter("from", from);
            inProgressQuery.setParameter("to", to);
            dto.inProgress = ((Number) inProgressQuery.getSingleResult()).longValue();
            
            // Cancelled workflows
            Query cancelledQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances " +
                "WHERE status = 'CANCELLED' " +
                "AND COALESCE(end_date, start_date, created_date) BETWEEN :from AND :to"
            );
            cancelledQuery.setParameter("from", from);
            cancelledQuery.setParameter("to", to);
            dto.cancelled = ((Number) cancelledQuery.getSingleResult()).longValue();
            
            // ✅ ENHANCED: Get overdue tasks (current snapshot)
            Query overdueQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_tasks " +
                "WHERE status = 'PENDING' AND due_date < CURRENT_TIMESTAMP"
            );
            dto.overdueTasks = ((Number) overdueQuery.getSingleResult()).longValue();
            
            // ✅ Average approval hours for completed workflows
            Query avgApprovalQuery = entityManager.createNativeQuery(
                "SELECT AVG(EXTRACT(EPOCH FROM (end_date - start_date))/3600.0) " +
                "FROM workflow_instances " +
                "WHERE status = 'APPROVED' AND end_date IS NOT NULL AND start_date IS NOT NULL " +
                "AND end_date BETWEEN :from AND :to"
            );
            avgApprovalQuery.setParameter("from", from);
            avgApprovalQuery.setParameter("to", to);
            Object avgResult = avgApprovalQuery.getSingleResult();
            dto.avgApprovalHours = avgResult != null ? round2(((Number) avgResult).doubleValue()) : null;
            
            log.info("✅ Analytics Results - Total: {}, Approved: {}, Rejected: {}, In Progress: {}", 
                    dto.total, dto.approved, dto.rejected, dto.inProgress);
            
        } catch (Exception e) {
            log.error("❌ Error getting overview analytics: {}", e.getMessage(), e);
            // Set safe defaults
            dto.total = 0L;
            dto.approved = 0L;
            dto.rejected = 0L;
            dto.inProgress = 0L;
            dto.cancelled = 0L;
            dto.overdueTasks = 0L;
            dto.avgApprovalHours = null;
        }

        return dto;
    }

    /**
     * ✅ FIXED: Real-time analytics bypass cache completely
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRealTimeAnalytics() {
        log.info("🔍 Getting real-time analytics with cache bypass");
        
        Map<String, Object> analytics = new HashMap<>();
        
        try {
            // ✅ Direct SQL queries to bypass ALL caches
            Query totalQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances");
            
            Query approvedQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances WHERE status = 'APPROVED'");
            
            Query inProgressQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances WHERE status = 'IN_PROGRESS'");
            
            Query rejectedQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_instances WHERE status = 'REJECTED'");
            
            long total = ((Number) totalQuery.getSingleResult()).longValue();
            long approved = ((Number) approvedQuery.getSingleResult()).longValue();
            long inProgress = ((Number) inProgressQuery.getSingleResult()).longValue();
            long rejected = ((Number) rejectedQuery.getSingleResult()).longValue();
            
            analytics.put("totalWorkflows", total);
            analytics.put("approvedWorkflows", approved);
            analytics.put("inProgressWorkflows", inProgress);
            analytics.put("rejectedWorkflows", rejected);
            analytics.put("timestamp", LocalDateTime.now());
            
            // ✅ List recent workflows for debugging
            Query recentQuery = entityManager.createNativeQuery(
                "SELECT id, title, status, updated_date FROM workflow_instances " +
                "ORDER BY updated_date DESC LIMIT 10");
            
            @SuppressWarnings("unchecked")
            List<Object[]> recentWorkflows = recentQuery.getResultList();
            analytics.put("recentWorkflows", recentWorkflows);
            
            log.info("📊 REAL-TIME Analytics - Total: {}, Approved: {}, In Progress: {}, Rejected: {}", 
                    total, approved, inProgress, rejected);
            
            return analytics;
            
        } catch (Exception e) {
            log.error("❌ Error getting real-time analytics: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * ✅ ENHANCED: Template metrics with direct database queries
     */
    public List<TemplateMetricsDTO> getByTemplate(LocalDateTime from, LocalDateTime to) {
        log.info("🔍 Getting template metrics - Date Range: {} to {}", from, to);
        
        try {
            // ✅ Use direct SQL with proper joins
            Query query = entityManager.createNativeQuery(
                "SELECT " +
                "t.id as template_id, " +
                "t.name as template_name, " +
                "COUNT(wi.id) as total, " +
                "COUNT(CASE WHEN wi.status = 'APPROVED' THEN 1 END) as approved, " +
                "COUNT(CASE WHEN wi.status = 'REJECTED' THEN 1 END) as rejected, " +
                "AVG(CASE WHEN wi.end_date IS NOT NULL AND wi.start_date IS NOT NULL " +
                "     THEN EXTRACT(EPOCH FROM (wi.end_date - wi.start_date))/3600.0 END) as avg_duration " +
                "FROM workflow_templates t " +
                "LEFT JOIN workflow_instances wi ON t.id = wi.template_id " +
                "  AND COALESCE(wi.end_date, wi.start_date, wi.created_date) BETWEEN :from AND :to " +
                "GROUP BY t.id, t.name " +
                "ORDER BY total DESC"
            );
            query.setParameter("from", from);
            query.setParameter("to", to);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            List<TemplateMetricsDTO> metrics = results.stream().map(row -> {
                TemplateMetricsDTO dto = new TemplateMetricsDTO();
                dto.templateId = row[0] != null ? row[0].toString() : "unknown";
                dto.templateName = row[1] != null ? row[1].toString() : "Unknown";
                dto.total = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                dto.approved = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                dto.rejected = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                dto.avgDurationHours = row[5] != null ? round2(((Number) row[5]).doubleValue()) : null;
                dto.approvalRate = dto.total > 0L ? round2((double) dto.approved / dto.total * 100) : null;
                return dto;
            }).collect(Collectors.toList());
            
            log.info("✅ Template metrics generated: {} templates", metrics.size());
            return metrics;
            
        } catch (Exception e) {
            log.error("❌ Error getting template metrics: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * ✅ FIXED: Step metrics with direct database queries
     */
    public List<StepMetricsDTO> getByStep(LocalDateTime from, LocalDateTime to) {
        log.info("🔍 Getting step metrics - Date Range: {} to {}", from, to);
        
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT " +
                "ws.step_order, " +
                "AVG(CASE WHEN wt.completed_date IS NOT NULL AND wt.created_date IS NOT NULL " +
                "     THEN EXTRACT(EPOCH FROM (wt.completed_date - wt.created_date))/3600.0 END) as avg_completion, " +
                "COUNT(wt.id) as total_tasks, " +
                "COUNT(CASE WHEN wt.status = 'COMPLETED' THEN 1 END) as completed_tasks, " +
                "COUNT(CASE WHEN wt.action = 'APPROVE' THEN 1 END) as approvals, " +
                "COUNT(CASE WHEN wt.action = 'REJECT' THEN 1 END) as rejections " +
                "FROM workflow_steps ws " +
                "LEFT JOIN workflow_tasks wt ON ws.id = wt.step_id " +
                "  AND wt.created_date BETWEEN :from AND :to " +
                "GROUP BY ws.step_order " +
                "ORDER BY ws.step_order"
            );
            query.setParameter("from", from);
            query.setParameter("to", to);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            List<StepMetricsDTO> metrics = results.stream().map(row -> {
                StepMetricsDTO dto = new StepMetricsDTO();
                dto.stepOrder = row[0] != null ? ((Number) row[0]).intValue() : null;
                dto.avgTaskCompletionHours = row[1] != null ? round2(((Number) row[1]).doubleValue()) : null;
                dto.totalTasks = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                dto.completedTasks = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                dto.approvals = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                dto.rejections = row[5] != null ? ((Number) row[5]).longValue() : 0L;
                dto.completionRate = dto.totalTasks > 0L ? 
                    round2((double) dto.completedTasks / dto.totalTasks * 100) : null;
                return dto;
            }).collect(Collectors.toList());
            
            log.info("✅ Step metrics generated: {} steps", metrics.size());
            return metrics;
            
        } catch (Exception e) {
            log.error("❌ Error getting step metrics: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * ✅ ENHANCED: User metrics with proper filtering
     */
    public MyMetricsDTO getMyMetrics(User currentUser, LocalDateTime from, LocalDateTime to) {
        log.info("🔍 Getting metrics for user {} - Date Range: {} to {}", 
                currentUser.getId(), from, to);
        
        MyMetricsDTO dto = new MyMetricsDTO();

        try {
            // User's workflows
            Query myWorkflowsQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*), " +
                "COUNT(CASE WHEN status = 'APPROVED' THEN 1 END), " +
                "COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) " +
                "FROM workflow_instances " +
                "WHERE initiated_by = :userId " +
                "AND COALESCE(end_date, start_date, created_date) BETWEEN :from AND :to"
            );
            myWorkflowsQuery.setParameter("userId", currentUser.getId());
            myWorkflowsQuery.setParameter("from", from);
            myWorkflowsQuery.setParameter("to", to);
            
            Object[] workflowResult = (Object[]) myWorkflowsQuery.getSingleResult();
            dto.myInitiatedTotal = workflowResult[0] != null ? ((Number) workflowResult[0]).longValue() : 0L;
            dto.myInitiatedApproved = workflowResult[1] != null ? ((Number) workflowResult[1]).longValue() : 0L;
            dto.myInitiatedRejected = workflowResult[2] != null ? ((Number) workflowResult[2]).longValue() : 0L;
            
            // User's tasks
            Query myTasksQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*), " +
                "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) " +
                "FROM workflow_tasks " +
                "WHERE assigned_to = :userId " +
                "AND created_date BETWEEN :from AND :to"
            );
            myTasksQuery.setParameter("userId", currentUser.getId());
            myTasksQuery.setParameter("from", from);
            myTasksQuery.setParameter("to", to);
            
            Object[] taskResult = (Object[]) myTasksQuery.getSingleResult();
            long totalMyTasks = taskResult[0] != null ? ((Number) taskResult[0]).longValue() : 0L;
            dto.myCompletedTasks = taskResult[1] != null ? ((Number) taskResult[1]).longValue() : 0L;
            
            // Current pending tasks
            Query pendingQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM workflow_tasks " +
                "WHERE assigned_to = :userId AND status = 'PENDING'"
            );
            pendingQuery.setParameter("userId", currentUser.getId());
            dto.myPendingTasks = ((Number) pendingQuery.getSingleResult()).longValue();
            
            // Task completion rate
            dto.myTaskCompletionRate = totalMyTasks > 0L ? 
                round2((double) dto.myCompletedTasks / totalMyTasks * 100) : null;
            
            log.info("✅ User metrics - Workflows: {}, Tasks completed: {}, Pending: {}", 
                    dto.myInitiatedTotal, dto.myCompletedTasks, dto.myPendingTasks);
            
        } catch (Exception e) {
            log.error("❌ Error getting user metrics: {}", e.getMessage(), e);
        }

        return dto;
    }

    /**
     * ✅ ENHANCED: CSV export methods
     */
    public byte[] exportOverviewCsv(LocalDateTime from, LocalDateTime to) {
        try {
            var overview = getOverview(from, to);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append(String.format("Date Range,%s to %s\n", from, to));
            csv.append(String.format("Total Workflows,%d\n", overview.total));
            csv.append(String.format("Approved,%d\n", overview.approved));
            csv.append(String.format("Rejected,%d\n", overview.rejected));
            csv.append(String.format("In Progress,%d\n", overview.inProgress));
            csv.append(String.format("Cancelled,%d\n", overview.cancelled));
            csv.append(String.format("Overdue Tasks,%d\n", overview.overdueTasks));
            csv.append(String.format("Avg Approval Hours,%s\n", 
                overview.avgApprovalHours != null ? overview.avgApprovalHours.toString() : "N/A"));
            csv.append(String.format("Export Timestamp,%s\n", LocalDateTime.now()));
            
            return csv.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("❌ Overview CSV export failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate overview CSV", e);
        }
    }

    // ===== Helper Methods =====

    private Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n");
        String escaped = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
