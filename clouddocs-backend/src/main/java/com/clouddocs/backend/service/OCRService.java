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

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 📖 OCR Service using Tesseract for free text extraction from images
 */
@Service
public class OCRService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRService.class);
    
    // ✅ SAFE: Class-level diagnostics flag
    private static volatile boolean diagnosticsRun = false;
    
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
     * ✅ SAFE: Initialize OCR Service without heavy operations
     */
    @PostConstruct
    public void initializeOCRService() {
        try {
            log.info("🔧 Initializing OCR Service...");
            
            // Quick environment check
            String tessDataPrefix = System.getenv("TESSDATA_PREFIX");
            String tesseractPath = System.getenv("TESSERACT_PATH");
            
            log.info("Environment: TESSDATA_PREFIX={}, TESSERACT_PATH={}", 
                    tessDataPrefix, tesseractPath);
            
            log.info("✅ OCR Service initialized successfully");
        } catch (Exception e) {
            log.error("❌ OCR Service initialization warning: {}", e.getMessage());
            // Don't throw - let service start anyway
        }
    }

    /**
     * ✅ SAFE: Extract text with lightweight diagnostics
     */
    public OCRResultDTO extractTextFromImage(MultipartFile file) {
        log.info("🔍 Starting OCR extraction for file: {}", file.getOriginalFilename());

        // ✅ SAFE: Run lightweight diagnostics once
        if (!diagnosticsRun) {
            synchronized (OCRService.class) {
                if (!diagnosticsRun) {
                    runSafeTessdataDiagnostics();
                    diagnosticsRun = true;
                }
            }
        }
        
        try {
            // Validate file type
            if (!isImageFile(file)) {
                throw new IllegalArgumentException("File must be an image (JPEG, PNG, BMP, TIFF, GIF)");
            }
            
            // Detect tessdata path
            String tessDataPath = detectTessdataPath();
            if (tessDataPath == null) {
                log.warn("⚠️ Tessdata not found, OCR unavailable");
                return new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, 
                    "OCR temporarily unavailable - language data not found");
            }
            
            // Create temporary file
            Path tempFile = createTempFile(file);
            
            try {
                // Initialize Tesseract with explicit datapath
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessDataPath);
                log.info("🔧 Using tessdata path: {}", tessDataPath);
                
                tesseract.setLanguage("eng");
                tesseract.setPageSegMode(1);
                tesseract.setOcrEngineMode(1);
                
                // Perform OCR
                long startTime = System.currentTimeMillis();
                String extractedText = tesseract.doOCR(tempFile.toFile());
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Clean up extracted text
                String cleanedText = cleanExtractedText(extractedText);
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

    /**
     * ✅ SAFE: Lightweight tessdata diagnostics - no filesystem recursion
     */
    private void runSafeTessdataDiagnostics() {
        log.info("=== SAFE TESSDATA DIAGNOSTIC REPORT ===");
        
        try {
            // Log environment variables
            log.info("Environment Variables:");
            log.info("  TESSDATA_PREFIX: {}", System.getenv("TESSDATA_PREFIX"));
            log.info("  TESSERACT_PATH: {}", System.getenv("TESSERACT_PATH"));
            log.info("  Operating System: {}", System.getProperty("os.name"));
            
            // ✅ SAFE: Check only specific known locations (no recursion)
            String[] specificPaths = {
                "/usr/share/tessdata",
                "/usr/share/tesseract-ocr/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/local/share/tessdata",
                "/opt/tesseract/tessdata"
            };
            
            log.info("Checking specific tessdata locations:");
            for (String path : specificPaths) {
                checkTessdataLocation(path);
            }
            
            // ✅ SAFE: Try command-line approach with timeout
            tryCommandLineSearch();
            
        } catch (Exception e) {
            log.warn("Diagnostics failed (non-critical): {}", e.getMessage());
        }
        
        log.info("=== END DIAGNOSTIC REPORT ===");
    }
    
    /**
     * ✅ SAFE: Check single location without recursion
     */
    private void checkTessdataLocation(String path) {
        try {
            File dir = new File(path);
            boolean exists = dir.exists();
            boolean readable = dir.canRead();
            
            log.info("  {} - exists: {}, readable: {}", path, exists, readable);
            
            if (exists && readable) {
                File[] files = dir.listFiles((file) -> 
                    file.isFile() && file.getName().endsWith(".traineddata"));
                
                if (files != null && files.length > 0) {
                    List<String> trainedDataFiles = Arrays.stream(files)
                            .map(File::getName)
                            .limit(5) // Limit to 5 files
                            .collect(Collectors.toList());
                    log.info("    Traineddata files: {}", trainedDataFiles);
                }
            }
        } catch (Exception e) {
            log.debug("Cannot check {}: {}", path, e.getMessage());
        }
    }
    
    /**
     * ✅ SAFE: Command-line search with strict timeout
     */
    private void tryCommandLineSearch() {
        try {
            log.info("Attempting command-line tessdata search...");
            
            ProcessBuilder pb = new ProcessBuilder("find", "/usr/share", "-name", "*.traineddata", "-maxdepth", "3");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // ✅ SAFE: Strict 3-second timeout
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            
            if (finished) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null && count < 3) {
                        log.info("Command found: {}", line);
                        count++;
                    }
                    
                    if (count == 0) {
                        log.info("Command search found no traineddata files");
                    }
                }
            } else {
                log.info("Command search timed out (non-critical)");
                process.destroyForcibly();
            }
            
        } catch (Exception e) {
            log.debug("Command-line search failed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * ✅ Your existing tessdata detection (unchanged)
     */
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
            
            // Check if tessdata files are directly in env path
            File directEngFile = new File(envPath, "eng.traineddata");
            if (directEngFile.exists()) {
                log.info("✅ Found tessdata directly at TESSDATA_PREFIX: {}", envPath);
                return new File(envPath).getParent();
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
            locations = new String[]{
                "/usr/share/tessdata",
                "/usr/share/tesseract-ocr/tessdata",
                "/usr/share/tesseract-ocr/4.00",
                "/usr/share/tesseract-ocr",
                "/usr/share",
                "/usr/local/share/tessdata",
                "/opt/tesseract/tessdata",
                "/app/tessdata"
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
            
            // Check if location itself contains eng.traineddata
            File directEngFile = new File(location, "eng.traineddata");
            if (directEngFile.exists()) {
                String parentPath = new File(location).getParent();
                log.info("✅ Found tessdata directly at: {} (parent: {})", location, parentPath);
                return parentPath != null ? parentPath : location;
            }
        }
        
        log.error("❌ No valid tessdata directory found for OS: {}", osName);
        return null;
    }

    /**
     * ✅ Check if Tesseract is properly configured
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

    // ✅ Keep all your existing helper methods unchanged:
    
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
            stats.put("tesseractAvailable", isTesseractAvailable());

            log.info("✅ OCR statistics built successfully");

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
    
    public DocumentWithOCRDTO processDocumentWithOCR(MultipartFile file, String description, String category) {
        log.info("📄 Processing document with OCR: {}", file.getOriginalFilename());
        
        try {
            OCRResultDTO ocrResult = extractTextFromImage(file);
            
            if (!ocrResult.isSuccess() || ocrResult.getExtractedText().length() < 10) {
                log.warn("⚠️ OCR extraction yielded minimal text for: {}", file.getOriginalFilename());
            }
            
            String embeddingContent = createEmbeddingContent(
                file.getOriginalFilename(), 
                description, 
                category, 
                ocrResult.getExtractedText()
            );
            
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
