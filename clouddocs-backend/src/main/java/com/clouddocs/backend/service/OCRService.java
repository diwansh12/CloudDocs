package com.clouddocs.backend.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.dto.DocumentWithOCRDTO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 📖 OCR Service using Tesseract for free text extraction from images
 */
@Service
public class OCRService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRService.class);
    
    // Supported image formats
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/tiff", "image/gif"
    );
    
    @Autowired
    private MultiProviderAIService aiService;

    private final DocumentRepository documentRepository;
    
    public OCRService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * ✅ UPDATED: Extract text from uploaded image file using OCR (Render-compatible)
     */
    public OCRResultDTO extractTextFromImage(MultipartFile file) {
        log.info("🔍 Starting OCR extraction for file: {}", file.getOriginalFilename());
        
        try {
            // Validate file type
            if (!isImageFile(file)) {
                throw new IllegalArgumentException("File must be an image (JPEG, PNG, BMP, TIFF, GIF)");
            }
            
            // ✅ CRITICAL FIX: Detect and validate tessdata path
            String tessDataPath = detectTessdataPath();
            if (tessDataPath == null) {
                log.warn("⚠️ Tessdata not found, OCR unavailable");
                return new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, 
                    "OCR temporarily unavailable - language data not found");
            }
            
            // Create temporary file
            Path tempFile = createTempFile(file);
            
            try {
                // ✅ CRITICAL: Initialize Tesseract with explicit datapath
                ITesseract tesseract = new Tesseract();
                
                // Set datapath explicitly for both Windows and Linux
                tesseract.setDatapath(tessDataPath);
                log.info("🔧 Using tessdata path: {}", tessDataPath);
                
                tesseract.setLanguage("eng"); // English language
                tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
                tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
                
                // Perform OCR
                long startTime = System.currentTimeMillis();
                String extractedText = tesseract.doOCR(tempFile.toFile());
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Clean up extracted text
                String cleanedText = cleanExtractedText(extractedText);
                
                // Calculate confidence score (basic implementation)
                double confidence = calculateConfidence(cleanedText);
                
                log.info("✅ OCR completed in {}ms. Extracted {} characters with {:.1f}% confidence", 
                    processingTime, cleanedText.length(), confidence * 100);
                
                return new OCRResultDTO(
                    cleanedText,
                    confidence,
                    processingTime,
                    file.getOriginalFilename(),
                    true
                );
                
            } finally {
                // Clean up temporary file
                Files.deleteIfExists(tempFile);
            }
            
        } catch (TesseractException e) {
            log.error("❌ Tesseract OCR failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            return new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, 
                "OCR processing failed: " + e.getMessage());
                
        } catch (Exception e) {
            log.error("❌ OCR processing failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            return new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, 
                "File processing failed: " + e.getMessage());
        }
    }

   private String detectTessdataPath() {
    // Check environment variable first
    String envPath = System.getenv("TESSDATA_PREFIX");
    if (envPath != null && !envPath.isEmpty()) {
        File tessDataDir = new File(envPath, "tessdata");
        File engFile = new File(tessDataDir, "eng.traineddata");
        if (tessDataDir.exists() && engFile.exists()) {
            log.info("✅ Using TESSDATA_PREFIX: {}", envPath);
            return envPath;
        }
        log.warn("⚠️ TESSDATA_PREFIX set but tessdata not found at: {}", envPath);
    }
    
    String osName = System.getProperty("os.name").toLowerCase();
    String[] locations;
    
    if (osName.contains("windows")) {
        locations = new String[]{
            "C:\\Program Files\\Tesseract-OCR",
            "C:\\Program Files (x86)\\Tesseract-OCR",
            "C:\\tesseract"
        };
    } else {
        // ✅ UPDATED: Add most common Linux locations first
        locations = new String[]{
            "/usr/share/tessdata",                    // Most common location
            "/usr/local/share/tessdata",             // Compiled from source
            "/usr/share/tesseract-ocr/tessdata",     // Alternative location  
            "/usr/share/tesseract-ocr/4.00",        // Version-specific
            "/usr/share/tesseract-ocr",              // General location
            "/usr/share",                            // Fallback
            "/app/tessdata"                          // Custom app location
        };
    }
    
    // Check each location
    for (String location : locations) {
        File tessDataDir = new File(location, "tessdata");
        File engFile = new File(tessDataDir, "eng.traineddata");
        
        if (tessDataDir.exists() && engFile.exists()) {
            log.info("✅ Auto-detected tessdata at: {} (OS: {})", location, osName);
            return location;
        }
        
        // Also check if location itself is tessdata directory
        File directEngFile = new File(location, "eng.traineddata");
        if (directEngFile.exists()) {
            String parentPath = new File(location).getParent();
            log.info("✅ Found tessdata directly at: {} (parent: {})", location, parentPath);
            return parentPath;
        }
    }
    
    log.error("❌ No valid tessdata directory found for OS: {}", osName);
    return null;
}


    /**
     * ✅ UPDATED: Check if Tesseract is properly configured
     */
    private boolean isTesseractAvailable() {
        try {
            String tessDataPath = detectTessdataPath();
            return tessDataPath != null;
        } catch (Exception e) {
            log.warn("Tesseract availability check failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getOCRStatistics(String username) {
        Map<String, Object> stats = new HashMap<>();

        try {
            log.info("📊 Fetching OCR statistics for user: {}", username);

            long totalDocuments = documentRepository.count();
            long documentsWithOCR = 0L;
            long documentsWithEmbeddings = 0L;
            long aiReadyDocuments = 0L;
            Double avgOCRConfidence = 0.0;

            try {
                documentsWithOCR = documentRepository.findByHasOcrTrue().size();
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch documentsWithOCR: {}", e.getMessage());
            }

            try {
                documentsWithEmbeddings = documentRepository.findByEmbeddingGeneratedTrue().size();
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch documentsWithEmbeddings: {}", e.getMessage());
            }

            try {
                aiReadyDocuments = documentRepository.countAIReadyDocumentsWithOCR(username);
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch aiReadyDocuments: {}", e.getMessage());
            }

            try {
                avgOCRConfidence = Optional.ofNullable(
                        documentRepository.getAverageOCRConfidenceByUser(username)
                ).orElse(0.0);
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch averageOCRConfidence: {}", e.getMessage());
            }

            double ocrCoverage = (totalDocuments > 0)
                    ? (documentsWithOCR * 100.0 / totalDocuments)
                    : 0.0;

            stats.put("totalDocuments", totalDocuments);
            stats.put("documentsWithOCR", documentsWithOCR);
            stats.put("documentsWithEmbeddings", documentsWithEmbeddings);
            stats.put("ocrCoverage", ocrCoverage);
            stats.put("averageOCRConfidence", avgOCRConfidence);
            stats.put("aiReadyDocuments", aiReadyDocuments);

            stats.put("timestamp", System.currentTimeMillis());
            stats.put("status", "success");
            stats.put("service", "OCR Service");
            stats.put("supportedFormats", Arrays.asList("JPEG", "PNG", "BMP", "TIFF", "GIF"));
            stats.put("tesseractAvailable", isTesseractAvailable()); // ✅ Updated to use real check

            log.info("✅ OCR statistics built successfully: {}", stats);

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
            stats.put("service", "OCR Service");
            stats.put("supportedFormats", Arrays.asList("JPEG", "PNG", "BMP", "TIFF", "GIF"));
            stats.put("tesseractAvailable", false);
        }

        return stats;
    }

    // ✅ Keep all your existing helper methods unchanged:
    
    public DocumentWithOCRDTO processDocumentWithOCR(MultipartFile file, String description, String category) {
        log.info("📄 Processing document with OCR: {}", file.getOriginalFilename());
        
        try {
            // Extract text using OCR
            OCRResultDTO ocrResult = extractTextFromImage(file);
            
            if (!ocrResult.isSuccess() || ocrResult.getExtractedText().length() < 10) {
                log.warn("⚠️ OCR extraction yielded minimal text for: {}", file.getOriginalFilename());
            }
            
            // Create enriched content for embedding
            String embeddingContent = createEmbeddingContent(
                file.getOriginalFilename(), 
                description, 
                category, 
                ocrResult.getExtractedText()
            );
            
            // Generate AI embedding
            List<Double> embedding = null;
            if (embeddingContent.length() > 20) {
                try {
                    embedding = aiService.generateEmbedding(embeddingContent);
                    log.info("✅ Generated embedding with {} dimensions", embedding.size());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to generate embedding: {}", e.getMessage());
                }
            }
            
            return new DocumentWithOCRDTO(
                file,
                ocrResult,
                embedding,
                embeddingContent,
                description,
                category
            );
            
        } catch (Exception e) {
            log.error("❌ Document processing with OCR failed: {}", e.getMessage());
            throw new RuntimeException("Document OCR processing failed", e);
        }
    }
    
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && SUPPORTED_FORMATS.contains(contentType.toLowerCase());
    }
    
    private Path createTempFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".") 
            ? originalName.substring(originalName.lastIndexOf(".")) 
            : ".tmp";
            
        Path tempFile = Files.createTempFile("ocr_", extension);
        file.transferTo(tempFile.toFile());
        return tempFile;
    }
    
    private String cleanExtractedText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        
        return rawText
            .replaceAll("\\s+", " ")
            .replaceAll("[\\u0000-\\u001F\\u007F]", "")
            .replaceAll("\\r\\n|\\r|\\n", " ")
            .trim();
    }
    
    private double calculateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        double score = 0.5;
        
        if (text.length() > 50) score += 0.2;
        
        long alphanumericCount = text.chars().filter(Character::isLetterOrDigit).count();
        double alphanumericRatio = (double) alphanumericCount / text.length();
        score += alphanumericRatio * 0.3;
        
        long specialCharCount = text.chars().filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)).count();
        double specialCharRatio = (double) specialCharCount / text.length();
        if (specialCharRatio > 0.3) score -= 0.2;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private String createEmbeddingContent(String filename, String description, String category, String ocrText) {
        StringBuilder content = new StringBuilder();
        
        if (filename != null) {
            String name = filename.replaceAll("\\.[^.]+$", "").replace("_", " ").replace("-", " ");
            content.append("Document: ").append(name).append(". ");
        }
        
        if (description != null && !description.trim().isEmpty()) {
            content.append("Description: ").append(description.trim()).append(". ");
        }
        
        if (category != null && !category.trim().isEmpty()) {
            content.append("Category: ").append(category.trim()).append(". ");
        }
        
        if (ocrText != null && !ocrText.trim().isEmpty()) {
            String truncatedText = ocrText.length() > 500 
                ? ocrText.substring(0, 500) + "..."
                : ocrText;
            content.append("Content: ").append(truncatedText.trim()).append(". ");
        }
        
        content.append("Type: Scanned document with extracted text. ");
        
        return content.toString().trim();
    }
}
