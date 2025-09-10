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
     * Extract text from uploaded image file using OCR
     */
    public OCRResultDTO extractTextFromImage(MultipartFile file) {
        log.info("🔍 Starting OCR extraction for file: {}", file.getOriginalFilename());
        
        try {
            // Validate file type
            if (!isImageFile(file)) {
                throw new IllegalArgumentException("File must be an image (JPEG, PNG, BMP, TIFF, GIF)");
            }
            
            // Create temporary file
            Path tempFile = createTempFile(file);
            
            try {
                // Initialize Tesseract
                ITesseract tesseract = new Tesseract();
                
                // Configure Tesseract (you'll need to download language data)
                // tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata"); // Linux
                // tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata"); // Windows
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
        stats.put("tesseractAvailable", true);

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


private boolean isTesseractAvailable() {
    try {
        // Simple availability check
        return true; // Adjust based on your setup
    } catch (Exception e) {
        log.warn("Tesseract availability check failed: {}", e.getMessage());
        return false;
    }
}

    /**
     * Enhanced document upload with OCR text extraction and AI embedding
     */
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
    
    /**
     * Check if file is a supported image format
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && SUPPORTED_FORMATS.contains(contentType.toLowerCase());
    }
    
    /**
     * Create temporary file from MultipartFile
     */
    private Path createTempFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".") 
            ? originalName.substring(originalName.lastIndexOf(".")) 
            : ".tmp";
            
        Path tempFile = Files.createTempFile("ocr_", extension);
        file.transferTo(tempFile.toFile());
        return tempFile;
    }
    
    /**
     * Clean and normalize extracted text
     */
    private String cleanExtractedText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        
        return rawText
            // Remove excessive whitespace
            .replaceAll("\\s+", " ")
            // Remove special OCR artifacts
            .replaceAll("[\\u0000-\\u001F\\u007F]", "")
            // Normalize line breaks
            .replaceAll("\\r\\n|\\r|\\n", " ")
            // Trim
            .trim();
    }
    
    /**
     * Calculate basic confidence score based on text characteristics
     */
    private double calculateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        double score = 0.5; // Base score
        
        // Boost for reasonable length
        if (text.length() > 50) score += 0.2;
        
        // Boost for alphanumeric content
        long alphanumericCount = text.chars().filter(Character::isLetterOrDigit).count();
        double alphanumericRatio = (double) alphanumericCount / text.length();
        score += alphanumericRatio * 0.3;
        
        // Reduce for excessive special characters
        long specialCharCount = text.chars().filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)).count();
        double specialCharRatio = (double) specialCharCount / text.length();
        if (specialCharRatio > 0.3) score -= 0.2;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Create enriched content for AI embedding generation
     */
    private String createEmbeddingContent(String filename, String description, String category, String ocrText) {
        StringBuilder content = new StringBuilder();
        
        // Document filename
        if (filename != null) {
            String name = filename.replaceAll("\\.[^.]+$", "").replace("_", " ").replace("-", " ");
            content.append("Document: ").append(name).append(". ");
        }
        
        // User description
        if (description != null && !description.trim().isEmpty()) {
            content.append("Description: ").append(description.trim()).append(". ");
        }
        
        // Category
        if (category != null && !category.trim().isEmpty()) {
            content.append("Category: ").append(category.trim()).append(". ");
        }
        
        // OCR extracted text (truncated if too long)
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
