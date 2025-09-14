package com.clouddocs.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.dto.DocumentWithOCRDTO;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🚫 OCR Service - DISABLED for Memory Optimization
 * Provides helpful error messages instead of processing
 */
@Service
public class OCRService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRService.class);
    
    // Supported image formats (for validation only)
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/tiff", "image/gif"
    );
    
  
    
    private final DocumentRepository documentRepository;
    
    public OCRService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * ✅ SAFE: Initialize OCR Service (disabled version)
     */
    @PostConstruct
    public void initializeOCRService() {
        try {
            log.info("🔧 Initializing OCR Service (DISABLED for memory optimization)...");
            log.info("💡 OCR processing disabled to stay within 512MB deployment limit");
            log.info("✅ OCR Service initialized - all requests will return disabled status");
        } catch (Exception e) {
            log.error("❌ OCR Service initialization warning: {}", e.getMessage());
        }
    }

    /**
     * 🚫 DISABLED: Extract text - returns helpful error message
     */
    public OCRResultDTO extractTextFromImage(MultipartFile file) {
        String filename = file != null ? file.getOriginalFilename() : "unknown";
        log.info("🚫 OCR extraction requested for {} but OCR is disabled", filename);

        return OCRResultDTO.builder()
            .success(false)
            .filename(filename)
            .extractedText("")
            .confidence(0.0)
            .processingTimeMs(0L)
            .errorMessage("OCR processing temporarily disabled for memory optimization on 512MB deployment")
            .build();
    }

    /**
     * 🚫 DISABLED: Process document with OCR - returns helpful error
     */
    public DocumentWithOCRDTO processDocumentWithOCR(MultipartFile file, String description, String category) {
        String filename = file != null ? file.getOriginalFilename() : "unknown";
        log.info("🚫 Document OCR processing requested for {} but OCR is disabled", filename);
        
        // Return disabled status
        OCRResultDTO ocrResult = OCRResultDTO.builder()
            .success(false)
            .filename(filename)
            .extractedText("")
            .confidence(0.0)
            .processingTimeMs(0L)
            .errorMessage("OCR processing disabled for memory optimization")
            .build();

        return DocumentWithOCRDTO.builder()
            .ocrResult(ocrResult)
            .embedding(null)
            .embeddingContent(null)
            .description(description)
            .category(category)
            .ocrEnabled(false)
            .embeddingGenerated(false)
            .processingMessage("OCR temporarily disabled. Use regular document upload at /api/documents/upload")
            .build();
    }

    /**
     * ✅ WORKING: Get OCR statistics (disabled version)
     */
    public Map<String, Object> getOCRStatistics(String username) {
        Map<String, Object> stats = new HashMap<>();

        try {
            log.info("📊 Fetching OCR statistics for user: {} (OCR disabled)", username);

            long totalDocuments = documentRepository.count();

            stats.put("totalDocuments", totalDocuments);
            stats.put("documentsWithOCR", 0); // OCR disabled
            stats.put("documentsWithEmbeddings", 0);
            stats.put("ocrCoverage", 0.0);
            stats.put("averageOCRConfidence", 0.0);
            stats.put("aiReadyDocuments", 0);

            stats.put("timestamp", System.currentTimeMillis());
            stats.put("status", "disabled");
            stats.put("service", "OCR Service (Memory Optimized)");
            stats.put("supportedFormats", SUPPORTED_FORMATS);
            stats.put("tesseractAvailable", false);
            stats.put("ocrEnabled", false);
            stats.put("memoryOptimization", Map.of(
                "reason", "OCR disabled for 512MB deployment limit compliance",
                "memorySaved", "~150-250MB during processing",
                "alternative", "AI semantic search available for text-based document discovery"
            ));

            log.info("✅ OCR statistics built successfully (disabled mode)");

        } catch (Exception e) {
            log.error("❌ Error while building OCR statistics: {}", e.getMessage(), e);
            
            stats.put("totalDocuments", 0);
            stats.put("documentsWithOCR", 0);
            stats.put("documentsWithEmbeddings", 0);
            stats.put("ocrCoverage", 0.0);
            stats.put("averageOCRConfidence", 0.0);
            stats.put("aiReadyDocuments", 0);
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("status", "error");
            stats.put("service", "OCR Service (Disabled)");
            stats.put("ocrEnabled", false);
        }

        return stats;
    }

    /**
     * ✅ UTILITY: Validate image file type
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && SUPPORTED_FORMATS.contains(contentType.toLowerCase());
    }

    /**
     * ✅ UTILITY: Check if OCR is available (always false)
     */
    public boolean isOcrAvailable() {
        return false; // Always disabled
    }

    /**
     * ✅ UTILITY: Get OCR service status
     */
    public Map<String, Object> getServiceStatus() {
        return Map.of(
            "ocrEnabled", false,
            "reason", "Memory optimization for 512MB deployment limit",
            "memorySaved", "~150-250MB by disabling OCR processing",
            "alternatives", Map.of(
                "documentUpload", "Available via /api/documents/upload",
                "aiSearch", "Fully functional semantic search",
                "textExtraction", "Not available - OCR disabled"
            ),
            "recommendation", "Use AI semantic search to find document content"
        );
    }

    /**
     * ✅ UTILITY: Validate file for OCR (returns validation only)
     */
    public Map<String, Object> validateFileForOCR(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of(
                "valid", false,
                "reason", "File is null or empty"
            );
        }

        if (!isImageFile(file)) {
            return Map.of(
                "valid", false,
                "reason", "File must be an image (JPEG, PNG, BMP, TIFF, GIF)",
                "supportedFormats", SUPPORTED_FORMATS
            );
        }

        return Map.of(
            "valid", true,
            "note", "File is valid but OCR processing is disabled for memory optimization"
        );
    }
}
