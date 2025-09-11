package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.OCRService;
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

/**
 * 📖 OCR Controller for FREE text extraction from images
 */
@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = {
    "https://cloud-docs-tan.vercel.app", 
    "http://localhost:3000",
    "http://localhost:3001"
}, allowCredentials = "true")
public class OCRController {

    private static final Logger logger = LoggerFactory.getLogger(OCRController.class);
    
    // ✅ File size and type constraints
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

    /**
     * Extract text from image using OCR (preview only)
     */
    @PostMapping("/extract")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OCRResultDTO> extractText(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("📤 OCR extraction request for: {}", file.getOriginalFilename());
            
            // Validate file before processing
            ResponseEntity<OCRResultDTO> validationResult = validateFile(file);
            if (validationResult != null) {
                return validationResult;
            }
            
            OCRResultDTO result = ocrService.extractTextFromImage(file);
            logger.info("✅ OCR extraction completed for: {}", file.getOriginalFilename());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Invalid request for OCR: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ OCR extraction failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false,
                            "OCR processing failed: " + e.getMessage()));
        }
    }

    /**
     * ✅ ENHANCED: Upload document with OCR processing - with comprehensive validation and error handling
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadDocumentWithOCR(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            logger.info("📤 OCR upload request for: {} from user: {}", 
                file.getOriginalFilename(), userDetails.getUsername());

            // ✅ CRITICAL: Comprehensive file validation
            Map<String, Object> validationError = validateFileForUpload(file);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }

            // ✅ CRITICAL: Memory check before processing
            Map<String, Object> memoryError = checkMemoryAvailability();
            if (memoryError != null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(memoryError);
            }

            // ✅ Force garbage collection before heavy processing
            System.gc();
            
            // Process document with OCR
            var documentWithOCR = ocrService.processDocumentWithOCR(file, description, category);

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

            logger.info("✅ OCR upload completed for: {}", file.getOriginalFilename());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Invalid request for OCR upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Invalid request",
                    "message", e.getMessage()
                ));
                
        } catch (RuntimeException e) {
            logger.error("❌ OCR upload failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Processing failed",
                    "message", "Document processing failed: " + e.getMessage()
                ));
                
        } catch (Exception e) {
            logger.error("❌ Unexpected error during OCR upload for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Internal server error",
                    "message", "An unexpected error occurred. Please try again later."
                ));
        }
    }

    /**
     * ✅ ENHANCED: Comprehensive file validation for uploads
     */
    private Map<String, Object> validateFileForUpload(MultipartFile file) {
        // Check if file exists
        if (file == null || file.isEmpty()) {
            logger.warn("Upload validation failed: Empty file");
            return Map.of(
                "success", false,
                "error", "Empty file",
                "message", "Please select a file to upload"
            );
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.warn("Upload validation failed: File too large - {} bytes", file.getSize());
            return Map.of(
                "success", false,
                "error", "File too large",
                "message", String.format("File size (%.1fMB) exceeds maximum limit of %.1fMB", 
                    file.getSize() / 1024.0 / 1024.0, MAX_FILE_SIZE / 1024.0 / 1024.0),
                "maxSize", MAX_FILE_SIZE
            );
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            logger.warn("Upload validation failed: Unsupported file type - {}", contentType);
            return Map.of(
                "success", false,
                "error", "Unsupported file type",
                "message", "Please upload an image file (JPEG, PNG, BMP, TIFF, or GIF)",
                "allowedTypes", ALLOWED_CONTENT_TYPES
            );
        }

        // Check filename
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
     * ✅ CRITICAL: Memory availability check to prevent crashes
     */
    private Map<String, Object> checkMemoryAvailability() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            logger.debug("Memory status: Used={:.1f}MB, Max={:.1f}MB, Usage={:.1f}%", 
                usedMemory / 1024.0 / 1024.0, maxMemory / 1024.0 / 1024.0, memoryUsagePercent);

            // Reject if memory usage is too high
            if (memoryUsagePercent > 85) {
                logger.warn("Upload rejected: High memory usage - {:.1f}%", memoryUsagePercent);
                return Map.of(
                    "success", false,
                    "error", "Server overloaded",
                    "message", "Server is currently processing other requests. Please try again in a moment.",
                    "retryAfter", 30
                );
            }

            return null; // Memory OK
        } catch (Exception e) {
            logger.warn("Memory check failed: {}", e.getMessage());
            return null; // Allow processing if check fails
        }
    }

    /**
     * ✅ Helper method for backward compatibility
     */
    private ResponseEntity<OCRResultDTO> validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new OCRResultDTO("", 0.0, 0L, "unknown", false, "File is empty"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest()
                .body(new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, 
                    "Unsupported file type: " + contentType));
        }

        return null; // No validation errors
    }

    /**
     * Fetch OCR statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getOCRStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> stats = new HashMap<>();
        try {
            String username = userDetails.getUsername();
            stats.putAll(ocrService.getOCRStatistics(username));

            stats.put("timestamp", System.currentTimeMillis());
            stats.put("status", "success");
            stats.put("service", "OCR Controller");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ Failed to fetch OCR statistics: {}", e.getMessage(), e);
            
            stats.put("totalDocuments", 0);
            stats.put("documentsWithOCR", 0);
            stats.put("documentsWithEmbeddings", 0);
            stats.put("ocrCoverage", 0.0);
            stats.put("averageOCRConfidence", 0.0);
            stats.put("aiReadyDocuments", 0);
            stats.put("error", "Failed to fetch OCR statistics");
            stats.put("message", "Service temporarily unavailable");
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("status", "error");
            stats.put("service", "OCR Controller");

            return ResponseEntity.status(500).body(stats);
        }
    }

    /**
     * ✅ OCR Health Check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("service", "OCR Controller");
            health.put("timestamp", System.currentTimeMillis());
            health.put("message", "OCR endpoints are responding");
            health.put("ocrServiceAvailable", (ocrService != null));
            
            // Add memory status to health check
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            
            health.put("memoryUsage", String.format("%.1f%%", memoryUsage));
            health.put("memoryStatus", memoryUsage > 85 ? "HIGH" : "NORMAL");
            
            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("❌ OCR health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
