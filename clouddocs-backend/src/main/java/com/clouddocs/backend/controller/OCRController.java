package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.OCRService;
import com.clouddocs.backend.service.DocumentService;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.dto.DocumentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 📖 OCR Controller for FREE text extraction from images
 */
@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OCRController {
    
    private static final Logger logger = LoggerFactory.getLogger(OCRController.class);
    
    @Autowired
    private OCRService ocrService;
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * Extract text from image using OCR (preview only)
     */
    @PostMapping("/extract")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OCRResultDTO> extractText(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("📤 OCR extraction request for: {}", file.getOriginalFilename());
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
    
   // In your OCRController.java - Update the getOCRStatistics method

@GetMapping("/stats")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getOCRStatistics() {
    try {
        logger.info("📊 OCR statistics requested");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get actual OCR statistics from service
            Map<String, Object> serviceStats = ocrService.getOCRStatistics();
            stats.putAll(serviceStats);
            
        } catch (Exception serviceException) {
            logger.warn("OCR service stats failed, using safe defaults: {}", serviceException.getMessage());
            
            // ✅ SAFE FALLBACK: Always return valid structure
            stats.put("totalDocuments", 0);
            stats.put("documentsWithOCR", 0);
            stats.put("documentsWithEmbeddings", 0);
            stats.put("ocrCoverage", 0.0);
            stats.put("averageOCRConfidence", 0.0);
            stats.put("aiReadyDocuments", 0);
        }
        
        // ✅ CRITICAL: Always include required fields
        stats.put("timestamp", System.currentTimeMillis());
        stats.put("status", "success");
        stats.put("service", "OCR Controller");
        
        logger.info("✅ OCR statistics returned successfully: {}", stats);
        return ResponseEntity.ok(stats);
        
    } catch (Exception e) {
        logger.error("❌ Failed to fetch OCR statistics: {}", e.getMessage(), e);
        
        // ✅ STRUCTURED ERROR RESPONSE
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Failed to fetch OCR statistics");
        errorResponse.put("message", "Service temporarily unavailable");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", "error");
        
        // ✅ SAFE DEFAULTS for frontend
        errorResponse.put("totalDocuments", 0);
        errorResponse.put("documentsWithOCR", 0);
        errorResponse.put("documentsWithEmbeddings", 0);
        errorResponse.put("ocrCoverage", 0.0);
        errorResponse.put("averageOCRConfidence", 0.0);
        errorResponse.put("aiReadyDocuments", 0);
        
        return ResponseEntity.status(500).body(errorResponse);
    }
}

    
    /**
     * ✅ NEW: OCR Health Check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "OCR Controller");
            health.put("timestamp", System.currentTimeMillis());
            health.put("message", "OCR endpoints are responding");
            
            // Check if OCR service is available
            boolean ocrAvailable = ocrService != null;
            health.put("ocrServiceAvailable", ocrAvailable);
            
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
    
    /**
     * Upload document with OCR processing and AI embedding
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentDTO> uploadWithOCR(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            logger.info("📤 OCR upload request for: {}", file.getOriginalFilename());
            
            // Process document with OCR
            var documentWithOCR = ocrService.processDocumentWithOCR(file, description, category);
            
            // Save document with OCR data
            DocumentDTO savedDocument = documentService.saveDocumentWithOCR(
                documentWithOCR, 
                userDetails.getUsername()
            );
            
            logger.info("✅ OCR upload completed for: {}", file.getOriginalFilename());
            return ResponseEntity.ok(savedDocument);
            
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Invalid request for OCR upload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("❌ OCR upload failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

