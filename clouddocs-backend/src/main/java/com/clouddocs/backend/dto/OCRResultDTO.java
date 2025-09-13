package com.clouddocs.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder 
@AllArgsConstructor
@NoArgsConstructor
public class OCRResultDTO {
    private String extractedText;
    private Double confidence;
    private Long processingTimeMs;
    private String filename;
    private Boolean success;
    private String errorMessage;
    
    // Constructor for successful extraction
    public OCRResultDTO(String extractedText, Double confidence, Long processingTimeMs, String filename, Boolean success) {
        this.extractedText = extractedText;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
        this.filename = filename;
        this.success = success;
    }

    // ✅ ADD: Custom getter for backward compatibility
    public Long getProcessingTime() {
        return this.processingTimeMs;
    }

    // ✅ ADD: Custom isSuccess() method for better API compatibility
    public boolean isSuccess() {
        return Boolean.TRUE.equals(this.success);
    }
    
    // ✅ ADD: Null-safe success checking
    public boolean isFailed() {
        return !isSuccess();
    }
    
    // ✅ ADD: Utility method for checking if OCR extracted meaningful text
    public boolean hasValidText() {
        return isSuccess() && extractedText != null && extractedText.trim().length() > 0;
    }
    
    // ✅ ADD: Get confidence as percentage string
    public String getConfidencePercentage() {
        return confidence != null ? String.format("%.1f%%", confidence * 100) : "N/A";
    }
    
    // ✅ ADD: Utility method to get error message safely
    public String getError() {
        return errorMessage;
    }
}
