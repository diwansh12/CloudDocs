package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.OCRService;
import com.clouddocs.backend.service.DocumentService;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.dto.DocumentDTO;
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

/**
 * 📖 OCR Controller for FREE text extraction from images
 */
@RestController
@RequestMapping("/ocr")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class OCRController {

    private static final Logger logger = LoggerFactory.getLogger(OCRController.class);

    private final OCRService ocrService;
    private final DocumentService documentService;

    // ✅ Constructor injection
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
