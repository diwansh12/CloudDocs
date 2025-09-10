package com.clouddocs.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OCRController {
    
    private static final Logger logger = LoggerFactory.getLogger(OCRController.class);
    
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractText(@RequestParam("file") MultipartFile file) {
        logger.info("OCR extract called for: {}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OCR Controller Working!");
        response.put("filename", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        response.put("extractedText", "Mock OCR result for: " + file.getOriginalFilename());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        logger.info("OCR upload called for: {}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OCR Upload Working!");
        response.put("document", Map.of(
            "id", System.currentTimeMillis(),
            "originalFilename", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "status", "PENDING",
            "hasOcr", true,
            "ocrText", "Mock OCR text from: " + file.getOriginalFilename()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.info("OCR stats called");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", 0);
        stats.put("documentsWithOCR", 0);
        stats.put("documentsWithEmbeddings", 0);
        stats.put("ocrCoverage", 0.0);
        stats.put("averageOCRConfidence", 0.0);
        stats.put("aiReadyDocuments", 0);
        stats.put("status", "working");
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "OCR Controller is working!");
        return ResponseEntity.ok(health);
    }
}
