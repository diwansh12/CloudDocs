package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.OCRService;
import com.clouddocs.backend.service.DocumentService;
import com.clouddocs.backend.dto.OCRResultDTO;
import com.clouddocs.backend.dto.DocumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 📖 OCR Controller for FREE text extraction from images
 */
@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OCRController {
    
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
            OCRResultDTO result = ocrService.extractTextFromImage(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new OCRResultDTO("", 0.0, 0L, file.getOriginalFilename(), false, 
                    "OCR processing failed: " + e.getMessage()));
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
            // Process document with OCR
            var documentWithOCR = ocrService.processDocumentWithOCR(file, description, category);
            
            // Save document with OCR data
            DocumentDTO savedDocument = documentService.saveDocumentWithOCR(
                documentWithOCR, 
                userDetails.getUsername()
            );
            
            return ResponseEntity.ok(savedDocument);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
