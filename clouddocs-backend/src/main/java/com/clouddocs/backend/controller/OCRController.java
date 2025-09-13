package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.OCRService;
import jakarta.servlet.http.HttpServletRequest;
import com.clouddocs.backend.service.DocumentService;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.dto.DocumentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/ocr")
@CrossOrigin(origins = {
    "https://cloud-docs-tan.vercel.app", 
    "http://localhost:3000"
}, allowCredentials = "true")
public class OCRController {

    private static final Logger logger = LoggerFactory.getLogger(OCRController.class);
    
    // ✅ File constraints
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/tiff", "image/gif"
    );

    private final OCRService ocrService;
    private final DocumentService documentService;

    public OCRController(OCRService ocrService, DocumentService documentService) {
        this.ocrService = ocrService;
        this.documentService = documentService;
    }

    @PostMapping("/extract")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> extractText(@RequestParam("file") MultipartFile file, 
                                        HttpServletRequest request) {
        String filename = (file != null) ? file.getOriginalFilename() : "unknown";
        String clientIp = getClientIpAddress(request);
        
        try {
            logger.info("🔍 OCR request from {} for file: {}", clientIp, filename);

            // ✅ Early validation
            Map<String, Object> validationError = validateFileForExtraction(file);
            if (validationError != null) {
                logger.warn("❌ Validation failed for {}: {}", filename, validationError);
                return ResponseEntity.badRequest().body(validationError);
            }

            // ✅ Connection health check
            if (request.isAsyncStarted() || !isConnectionActive(request)) {
                logger.warn("❌ Connection not active for file: {}", filename);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(Map.of(
                        "success", false,
                        "error", "Connection timeout",
                        "message", "Upload connection timed out. Please try again."
                    ));
            }

            // ✅ UPDATED: Enhanced memory check with detailed logging
            double memoryUsage = getMemoryUsagePercent();
            logger.info("💾 Current memory usage: {:.1f}% for file: {}", memoryUsage, filename);
            
            if (memoryUsage > 90.0) {  // ✅ INCREASED from 85% to 90%
                logger.warn("❌ Memory usage too high ({:.1f}%) for file: {}", memoryUsage, filename);
                logDetailedMemoryStatus(); // ✅ NEW: Detailed memory logging
                
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "success", false, 
                        "error", "Server busy", 
                        "message", "Server memory usage high (" + String.format("%.1f", memoryUsage) + "%). Please try a smaller image or try again later.",
                        "memoryUsage", String.format("%.1f%%", memoryUsage),
                        "threshold", "90%"
                    ));
            }

            // ✅ Allow OCR processing
            logger.info("✅ Memory check passed ({:.1f}%), proceeding with OCR for: {}", memoryUsage, filename);

            // ✅ Process OCR with monitoring
            logger.info("🔍 Starting OCR processing for: {}", filename);
            OCRResultDTO result = ocrService.extractTextFromImage(file);
            
            if (result == null || !result.isSuccess()) {
                logger.error("❌ OCR processing failed for: {}", filename);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "OCR failed", 
                        "message", "Text extraction failed. Please try with a different image."));
            }

            logger.info("✅ OCR completed successfully for: {} ({}ms)", 
                filename, result.getProcessingTime());
            
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ OCR processing error for {} from {}: {}", filename, clientIp, e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Processing failed",
                    "message", "An error occurred during text extraction. Please try again."
                ));
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadDocumentWithOCR(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // ✅ SAFE: Get filename with null check first
            String filename = (file != null) ? file.getOriginalFilename() : "unknown";
            String username = (userDetails != null) ? userDetails.getUsername() : "unknown user";
            
            logger.info("📤 OCR upload request for: {} from user: {}", filename, username);

            // ✅ CRITICAL: Validate user authentication
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "User not authenticated"));
            }

            // ✅ CRITICAL: Comprehensive file validation (includes null check)
            Map<String, Object> validationError = validateFileForUpload(file);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }

            // ✅ UPDATED: Enhanced memory check for upload
            double memoryUsage = getMemoryUsagePercent();
            logger.info("💾 Current memory usage: {:.1f}% for upload: {}", memoryUsage, filename);
            
            if (memoryUsage > 90.0) {  // ✅ INCREASED from 85% to 90%
                logger.warn("❌ Memory usage too high ({:.1f}%) for upload: {}", memoryUsage, filename);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "success", false,
                        "error", "Server overloaded",
                        "message", "Server memory usage high (" + String.format("%.1f", memoryUsage) + "%). Please try again."
                    ));
            }

            logger.info("✅ Memory check passed ({:.1f}%), proceeding with upload for: {}", memoryUsage, filename);

            // ✅ Force garbage collection before heavy processing
            System.gc();
            
            // Process document with OCR (file is guaranteed not null by validation)
            var documentWithOCR = ocrService.processDocumentWithOCR(file, description, category);
            if (documentWithOCR == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "error", "Processing failed",
                        "message", "Document processing returned null result"
                    ));
            }

            // Save document with OCR data
            DocumentDTO savedDocument = documentService.saveDocumentWithOCR(
                    documentWithOCR,
                    userDetails.getUsername()
            );

            // ✅ Success response with detailed information
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document uploaded and processed successfully");
            response.put("document", savedDocument);
            response.put("ocrText", documentWithOCR.getOcrResult().getExtractedText());
            response.put("confidence", documentWithOCR.getOcrResult().getConfidence());
            response.put("processingTime", documentWithOCR.getOcrResult().getProcessingTime());
            response.put("embeddingGenerated", documentWithOCR.getEmbedding() != null);

            logger.info("✅ OCR upload completed for: {}", filename);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // ✅ SAFE: Get filename with null check in catch block
            String filename = (file != null) ? file.getOriginalFilename() : "unknown";
            logger.error("❌ OCR upload failed for {}: {}", filename, e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Upload processing failed",
                    "message", "An unexpected error occurred during upload processing"
                ));
        }
    }

    // ✅ NEW: Helper method to get memory usage percentage
    private double getMemoryUsagePercent() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            return ((double) usedMemory / maxMemory) * 100.0;
        } catch (Exception e) {
            logger.warn("Failed to get memory usage: {}", e.getMessage());
            return 0.0;
        }
    }

    // ✅ NEW: Detailed memory status logging
    private void logDetailedMemoryStatus() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            logger.info("🧠 Detailed Memory Status:");
            logger.info("  Max Memory: {} MB", maxMemory / 1024 / 1024);
            logger.info("  Total Memory: {} MB", totalMemory / 1024 / 1024);
            logger.info("  Used Memory: {} MB", usedMemory / 1024 / 1024);
            logger.info("  Free Memory: {} MB", freeMemory / 1024 / 1024);
            logger.info("  Usage Percentage: {:.1f}%", ((double) usedMemory / maxMemory) * 100);
        } catch (Exception e) {
            logger.warn("Failed to log detailed memory status: {}", e.getMessage());
        }
    }

    // Helper methods
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isConnectionActive(HttpServletRequest request) {
        try {
            // Check if connection is still active
            return !request.isAsyncStarted() && request.getInputStream().available() >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ✅ ENHANCED: Comprehensive file validation for extraction
     */
    private Map<String, Object> validateFileForExtraction(MultipartFile file) {
        if (file == null) {
            logger.warn("❌ File parameter is null");
            return Map.of(
                "success", false,
                "error", "Missing file",
                "message", "No file was provided in the request"
            );
        }

        if (file.isEmpty()) {
            logger.warn("❌ File is empty: {}", file.getOriginalFilename());
            return Map.of(
                "success", false,
                "error", "Empty file",
                "message", "The uploaded file is empty"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            logger.warn("❌ Invalid file type: {}", contentType);
            return Map.of(
                "success", false,
                "error", "Invalid file type",
                "message", "Please upload an image file (JPEG, PNG, BMP, TIFF, or GIF)",
                "allowedTypes", ALLOWED_CONTENT_TYPES
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            logger.warn("❌ File too large: {} bytes", file.getSize());
            return Map.of(
                "success", false,
                "error", "File too large",
                "message", String.format("File size (%.1fMB) exceeds maximum limit of %.1fMB", 
                    file.getSize() / 1024.0 / 1024.0, MAX_FILE_SIZE / 1024.0 / 1024.0)
            );
        }

        return null; // No validation errors
    }

    /**
     * ✅ ENHANCED: Comprehensive file validation for uploads
     */
    private Map<String, Object> validateFileForUpload(MultipartFile file) {
        Map<String, Object> extractionError = validateFileForExtraction(file);
        if (extractionError != null) {
            return extractionError;
        }

        // Additional validation for upload
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            return Map.of(
                "success", false,
                "error", "Invalid filename",
                "message", "File must have a valid name"
            );
        }

        return null; // No validation errors
    }

    /**
     * ✅ UPDATED: Simplified memory availability check - REMOVED (using direct percentage check now)
     */
    private boolean checkMemoryAvailable() {
        return getMemoryUsagePercent() < 90.0;  // ✅ UPDATED to 90%
    }

    /**
     * ✅ UPDATED: Enhanced health check with new threshold
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("service", "OCR Controller");
            health.put("timestamp", System.currentTimeMillis());
            health.put("ocrServiceAvailable", ocrService != null);
            health.put("documentServiceAvailable", documentService != null);
            
            // ✅ UPDATED: Memory status with new threshold
            double memoryUsage = getMemoryUsagePercent();
            health.put("memoryUsage", String.format("%.1f%%", memoryUsage));
            health.put("memoryStatus", memoryUsage > 90 ? "HIGH" : "NORMAL");  // ✅ UPDATED threshold
            health.put("memoryThreshold", "90%");  // ✅ NEW: Show current threshold
            
            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("❌ Health check failed: {}", e.getMessage(), e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(health);
        }
    }

    /**
     * ✅ OCR Statistics endpoint
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getOCRStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
            }

            String username = userDetails.getUsername();
            Map<String, Object> stats = ocrService.getOCRStatistics(username);
            
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("status", "success");
            
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ Failed to fetch OCR statistics: {}", e.getMessage(), e);
            
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Failed to fetch OCR statistics");
            errorStats.put("message", "Service temporarily unavailable");
            errorStats.put("timestamp", System.currentTimeMillis());
            errorStats.put("status", "error");
            
            return ResponseEntity.status(500).body(errorStats);
        }
    }

    @GetMapping("/network-status")
    public ResponseEntity<Map<String, Object>> networkStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("timestamp", System.currentTimeMillis());
            status.put("clientIp", getClientIpAddress(request));
            status.put("userAgent", request.getHeader("User-Agent"));
            status.put("contentLength", request.getContentLength());
            
            // ✅ UPDATED: Memory info with detailed logging
            double memoryUsage = getMemoryUsagePercent();
            Runtime runtime = Runtime.getRuntime();
            status.put("memoryUsedMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            status.put("memoryMaxMB", runtime.maxMemory() / 1024 / 1024);
            status.put("memoryUsagePercent", String.format("%.1f%%", memoryUsage));
            
            // Server info
            status.put("activeConnections", "Available");
            status.put("serverStatus", "OK");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
            status.put("serverStatus", "ERROR");
            return ResponseEntity.status(500).body(status);
        }
    }
}
