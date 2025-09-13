package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.OCRResultDTO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;        // ✅ ADD THIS IMPORT
import java.util.HashMap;    // ✅ ADD THIS IMPORT

@Service
public class OptimizedOCRService {

    private static final Logger log = LoggerFactory.getLogger(OptimizedOCRService.class);
    
    // ✅ Configurable optimization parameters
    @Value("${app.ocr.max-dimension:1200}")
    private int maxDimension;
    
    @Value("${app.ocr.max-file-size-mb:2}")
    private int maxFileSizeMB;
    
    @Value("${app.ocr.enable-compression:true}")
    private boolean enableCompression;
    
    @Value("${app.ocr.dpi-threshold:300}")
    private int dpiThreshold;
    
    // ✅ Memory-optimized constants
    private static final int MEMORY_THRESHOLD_PERCENT = 80;
    private static final long MAX_PROCESSING_TIME_MS = 30000; // 30 seconds timeout
    
    /**
     * ✅ MAIN METHOD: Extract text with comprehensive optimization
     */
    public OCRResultDTO extractTextOptimized(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        String filename = file.getOriginalFilename();
        
        try {
            log.info("🔧 Starting optimized OCR for: {} ({}KB)", filename, file.getSize() / 1024);
            
            // ✅ Pre-processing validation
            OCRResultDTO validationResult = validateAndPrepare(file);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            // ✅ Memory check before processing
            if (!checkMemoryAvailable()) {
                log.warn("❌ Insufficient memory for OCR processing: {}", filename);
                return OCRResultDTO.builder()
                    .success(false)
                    .extractedText("")
                    .confidence(0.0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .errorMessage("Server memory too high - please try again in a moment")
                    .build();
            }
            
            // ✅ Process with optimization pipeline
            BufferedImage optimizedImage = createOptimizedImage(file);
            if (optimizedImage == null) {
                return OCRResultDTO.builder()
                    .success(false)
                    .extractedText("")
                    .confidence(0.0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .errorMessage("Image preprocessing failed")
                    .build();
            }
            
            // ✅ Perform OCR with optimized settings
            String extractedText = performOptimizedOCR(optimizedImage);
            double confidence = calculateConfidence(extractedText);
            
            // ✅ Cleanup immediately
            cleanupResources(optimizedImage);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ OCR completed for: {} in {}ms (confidence: {:.1f}%)", 
                filename, processingTime, confidence * 100);
            
            return OCRResultDTO.builder()
                .success(true)
                .extractedText(extractedText.trim())
                .confidence(confidence)
                .processingTimeMs(processingTime)
                .build();
                
        } catch (OutOfMemoryError e) {
            log.error("❌ OCR out of memory for {}: {}", filename, e.getMessage());
            forceMemoryCleanup();
            
            return OCRResultDTO.builder()
                .success(false)
                .extractedText("")
                .confidence(0.0)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .errorMessage("Processing requires more memory than available")
                .build();
                
        } catch (Exception e) {
            log.error("❌ OCR processing failed for {}: {}", filename, e.getMessage(), e);
            
            return OCRResultDTO.builder()
                .success(false)
                .extractedText("")
                .confidence(0.0)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .errorMessage("OCR processing failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * ✅ VALIDATION: Comprehensive file validation
     */
    private OCRResultDTO validateAndPrepare(MultipartFile file) {
        // File size check
        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > maxFileSizeMB) {
            return OCRResultDTO.builder()
                .success(false)
                .extractedText("")
                .confidence(0.0)
                .errorMessage(String.format("File size (%.1fMB) exceeds limit of %dMB for free tier", 
                    (double) fileSizeInMB, maxFileSizeMB))
                .build();
        }
        
        // Content type check
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return OCRResultDTO.builder()
                .success(false)
                .extractedText("")
                .confidence(0.0)
                .errorMessage("File must be an image (JPEG, PNG, BMP, TIFF, or GIF)")
                .build();
        }
        
        return OCRResultDTO.builder().success(true).build();
    }
    
    /**
     * ✅ IMAGE OPTIMIZATION: Create memory-efficient image
     */
    private BufferedImage createOptimizedImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = null;
        BufferedImage optimizedImage = null;
        
        try {
            // ✅ Read original image
            originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                log.error("❌ Failed to read image from file");
                return null;
            }
            
            log.debug("📐 Original image: {}x{} pixels", 
                originalImage.getWidth(), originalImage.getHeight());
            
            // ✅ Resize if too large
            optimizedImage = resizeImageForOCR(originalImage);
            
            // ✅ Convert to optimal format for OCR
            if (enableCompression) {
                optimizedImage = convertToOptimalFormat(optimizedImage);
            }
            
            log.debug("📐 Optimized image: {}x{} pixels", 
                optimizedImage.getWidth(), optimizedImage.getHeight());
            
            return optimizedImage;
            
        } finally {
            // ✅ Always cleanup original image
            if (originalImage != null) {
                originalImage.flush();
            }
        }
    }
    
    /**
     * ✅ RESIZING: Smart image resizing for memory optimization
     */
    private BufferedImage resizeImageForOCR(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        // ✅ Skip if already optimal size
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return copyImage(original);
        }
        
        // ✅ Calculate optimal scale factor
        double scaleFactor = Math.min(
            (double) maxDimension / originalWidth,
            (double) maxDimension / originalHeight
        );
        
        int newWidth = (int) (originalWidth * scaleFactor);
        int newHeight = (int) (originalHeight * scaleFactor);
        
        log.debug("🔄 Resizing from {}x{} to {}x{} (scale: {:.2f})", 
            originalWidth, originalHeight, newWidth, newHeight, scaleFactor);
        
        // ✅ Create resized image with high quality
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        
        // ✅ High-quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resized;
    }
    
    /**
     * ✅ FORMAT CONVERSION: Convert to OCR-friendly format
     */
    private BufferedImage convertToOptimalFormat(BufferedImage image) {
        // ✅ Convert to RGB if not already (Tesseract prefers RGB)
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        
        BufferedImage convertedImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = convertedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        
        // ✅ Dispose original if different
        if (convertedImage != image) {
            image.flush();
        }
        
        return convertedImage;
    }
    
    /**
     * ✅ OCR PROCESSING: Perform OCR with optimized Tesseract settings
     */
    private String performOptimizedOCR(BufferedImage image) throws TesseractException {
        ITesseract tesseract = new Tesseract();
        
        // ✅ Optimized Tesseract configuration
        tesseract.setDatapath("/usr/share/tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(1);  // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only (faster)
        
        // ✅ Performance optimizations
        tesseract.setVariable("tessedit_char_whitelist", 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,!?@#$%^&*()-_=+[]{}|;:'\",.<>/");
        tesseract.setVariable("classify_bln_numeric_mode", "1");
        
        // ✅ Execute OCR
        String result = tesseract.doOCR(image);
        
        return result != null ? result : "";
    }
    
    /**
     * ✅ CONFIDENCE CALCULATION: Smart confidence estimation
     */
    private double calculateConfidence(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return 0.0;
        }
        
        String cleanText = extractedText.trim();
        int totalChars = cleanText.length();
        
        if (totalChars == 0) return 0.0;
        
        // ✅ Count readable characters
        int readableChars = 0;
        int words = 0;
        
        for (char c : cleanText.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || 
                ".,!?@#$%^&*()-_=+[]{}|;:'\",.<>/".indexOf(c) >= 0) {
                readableChars++;
            }
        }
        
        // ✅ Count words
        words = cleanText.split("\\s+").length;
        
        // ✅ Calculate confidence based on multiple factors
        double charRatio = (double) readableChars / totalChars;
        double lengthFactor = Math.min(1.0, totalChars / 50.0); // Better confidence for longer texts
        double wordFactor = words > 0 ? Math.min(1.0, words / 10.0) : 0.1;
        
        double confidence = charRatio * 0.6 + lengthFactor * 0.2 + wordFactor * 0.2;
        
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * ✅ MEMORY MANAGEMENT: Check available memory
     */
    private boolean checkMemoryAvailable() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            log.debug("💾 Memory usage: {:.1f}% ({}/{}MB)", 
                memoryUsagePercent, 
                usedMemory / 1024 / 1024, 
                maxMemory / 1024 / 1024);
            
            return memoryUsagePercent < MEMORY_THRESHOLD_PERCENT;
            
        } catch (Exception e) {
            log.warn("⚠️ Memory check failed: {}", e.getMessage());
            return true; // Assume OK if check fails
        }
    }
    
    /**
     * ✅ CLEANUP: Safe resource cleanup
     */
    private void cleanupResources(BufferedImage... images) {
        for (BufferedImage image : images) {
            if (image != null) {
                image.flush();
            }
        }
        
        // ✅ Suggest garbage collection
        System.gc();
    }
    
    /**
     * ✅ EMERGENCY CLEANUP: Force memory cleanup
     */
    private void forceMemoryCleanup() {
        try {
            // ✅ Force garbage collection multiple times
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(100);
            }
            
            Runtime runtime = Runtime.getRuntime();
            log.info("🧹 Forced cleanup - Memory: {:.1f}% used", 
                ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100);
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * ✅ UTILITY: Copy image safely
     */
    private BufferedImage copyImage(BufferedImage original) {
        BufferedImage copy = new BufferedImage(
            original.getWidth(), original.getHeight(), original.getType());
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        return copy;
    }
    
    /**
     * ✅ PUBLIC API: Get optimization statistics
     */
    public Map<String, Object> getOptimizationStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("memoryUsagePercent", (double) usedMemory / maxMemory * 100);
        stats.put("memoryUsedMB", usedMemory / 1024 / 1024);
        stats.put("memoryMaxMB", maxMemory / 1024 / 1024);
        stats.put("maxDimension", maxDimension);
        stats.put("maxFileSizeMB", maxFileSizeMB);
        stats.put("compressionEnabled", enableCompression);
        stats.put("timestamp", Instant.now().toString());
        
        return stats;
    }
}
