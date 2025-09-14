package com.clouddocs.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ocr")
@CrossOrigin(origins = {
    "https://cloud-docs-tan.vercel.app", 
    "http://localhost:3000",
    "http://localhost:3001"
}, allowCredentials = "true")
public class OCRController {

    private static final Logger logger = LoggerFactory.getLogger(OCRController.class);

    /**
     * 🚫 OCR DISABLED: Extract text endpoint
     */
    @PostMapping("/extract")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> extractText(@RequestParam("file") MultipartFile file) {
        String filename = (file != null) ? file.getOriginalFilename() : "unknown";
        
        logger.info("🚫 OCR extract request received for: {} (OCR disabled)", filename);
        
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "OCR temporarily disabled for memory optimization",
            "reason", "Feature requires more than 512MB RAM available on current hosting plan",
            "alternative", "Document storage and AI semantic search remain fully functional",
            "extractedText", "",
            "confidence", 0.0,
            "processingTimeMs", 0L,
            "status", "disabled",
            "suggestion", "Use regular document upload and AI search instead"
        ));
    }

    /**
     * 🚫 OCR DISABLED: Upload with OCR endpoint
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadDocumentWithOCR(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        String filename = (file != null) ? file.getOriginalFilename() : "unknown";
        String username = (userDetails != null) ? userDetails.getUsername() : "unknown";
        
        logger.info("🚫 OCR upload request received for: {} by {} (OCR disabled)", filename, username);

        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "OCR upload temporarily disabled for memory optimization",
            "reason", "Feature requires more than 512MB RAM available on current hosting plan",
            "alternative", "Use regular document upload instead - AI search will still work",
            "suggestion", "Upload document via /api/documents/upload endpoint",
            "note", "All other document management features remain fully functional"
        ));
    }

    /**
     * ✅ HEALTH CHECK: Shows disabled status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "OCR Controller");
        health.put("ocrEnabled", false);
        health.put("reason", "Disabled for 512MB memory compliance");
        health.put("timestamp", System.currentTimeMillis());
        health.put("memoryOptimized", true);
        health.put("alternativeFeatures", Map.of(
            "documentUpload", "Available via /api/documents/upload",
            "aiSearch", "Fully functional",
            "documentManagement", "All CRUD operations available"
        ));
        
        return ResponseEntity.ok(health);
    }

    /**
     * 🚫 OCR DISABLED: Statistics endpoint
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getOCRStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
            "status", "disabled",
            "message", "OCR statistics unavailable - feature disabled",
            "reason", "Memory optimization for 512MB deployment constraint",
            "documentsProcessed", 0,
            "ocrEnabled", false,
            "timestamp", System.currentTimeMillis(),
            "alternative", "Use /api/documents for document statistics"
        ));
    }

    /**
     * ✅ NETWORK STATUS: Shows disabled status
     */
    @GetMapping("/network-status")
    public ResponseEntity<Map<String, Object>> networkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", System.currentTimeMillis());
        status.put("ocrService", "disabled");
        status.put("reason", "Memory optimization");
        status.put("serverStatus", "OK");
        status.put("memoryOptimized", true);
        status.put("alternativeServices", "Document management and AI search available");
        
        return ResponseEntity.ok(status);
    }
}

