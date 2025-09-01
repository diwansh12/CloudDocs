package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.analytics.MyMetricsDTO;
import com.clouddocs.backend.dto.analytics.OverviewMetricsDTO;
import com.clouddocs.backend.dto.analytics.StepMetricsDTO;
import com.clouddocs.backend.dto.analytics.TemplateMetricsDTO;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.service.WorkflowAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/workflows/metrics")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class WorkflowAnalyticsController {

    @Autowired private WorkflowAnalyticsService analyticsService;
    @Autowired private UserRepository userRepository;

    private LocalDateTime defaultFrom() { return LocalDateTime.now().minusDays(90); }
    private LocalDateTime defaultTo() { return LocalDateTime.now(); }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER', 'USER')")
    @GetMapping("/overview")
    public ResponseEntity<OverviewMetricsDTO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var dto = analyticsService.getOverview(from != null ? from : defaultFrom(), to != null ? to : defaultTo());
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/by-template")
    public ResponseEntity<List<TemplateMetricsDTO>> byTemplate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var list = analyticsService.getByTemplate(from != null ? from : defaultFrom(), to != null ? to : defaultTo());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/by-step")
    public ResponseEntity<List<StepMetricsDTO>> byStep(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var list = analyticsService.getByStep(from != null ? from : defaultFrom(), to != null ? to : defaultTo());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<MyMetricsDTO> myMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var current = getCurrentUser();
        var dto = analyticsService.getMyMetrics(current, from != null ? from : defaultFrom(), to != null ? to : defaultTo());
        return ResponseEntity.ok(dto);
    }


@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@GetMapping(value = "/by-template/export", produces = "text/csv")
public ResponseEntity<byte[]> exportByTemplateCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

    var list = analyticsService.getByTemplate(
            from != null ? from : defaultFrom(),
            to != null ? to : defaultTo()
    );

    StringBuilder sb = new StringBuilder();
    sb.append("templateId,templateName,total,approved,rejected,avgDurationHours\n");
    for (var m : list) {
        sb.append(m.templateId).append(",")
          .append(csv(m.templateName)).append(",")
          .append(m.total).append(",")
          .append(m.approved).append(",")
          .append(m.rejected).append(",")
          .append(m.avgDurationHours == null ? "" : m.avgDurationHours).append("\n");
    }

    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template-metrics.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
}

@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@GetMapping(value = "/by-step/export", produces = "text/csv")
public ResponseEntity<byte[]> exportByStepCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

    var list = analyticsService.getByStep(
            from != null ? from : defaultFrom(),
            to != null ? to : defaultTo()
    );

    StringBuilder sb = new StringBuilder();
    sb.append("stepOrder,avgTaskCompletionHours,approvals,rejections\n");
    for (var m : list) {
        sb.append(m.stepOrder == null ? "" : m.stepOrder).append(",")
          .append(m.avgTaskCompletionHours == null ? "" : m.avgTaskCompletionHours).append(",")
          .append(m.approvals).append(",")
          .append(m.rejections).append("\n");
    }

    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=step-metrics.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
}

@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@GetMapping(value = "/overview/export", produces = "text/csv")
public ResponseEntity<byte[]> exportOverviewCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
    
    try {
        byte[] csvData = analyticsService.exportOverviewCsv(
            from != null ? from : defaultFrom(),
            to != null ? to : defaultTo()
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overview-metrics.csv");
        
        return ResponseEntity.ok().headers(headers).body(csvData);
        
    } catch (Exception e) {
        // Log the actual error for debugging
        System.err.println("❌ Overview CSV export failed: " + e.getMessage());
        e.printStackTrace();
        
        // Return a proper error response instead of letting it bubble up as 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body("CSV export failed: No data available for the selected date range".getBytes());
    }
}

// Helper to escape CSV fields with commas/quotes/newlines
private String csv(String s) {
    if (s == null) return "";
    boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
    String escaped = s.replace("\"", "\"\"");
    return needsQuotes ? "\"" + escaped + "\"" : escaped;
}


    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
    }
}


