package com.clouddocs.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentWithOCRDTO {
    private MultipartFile file;
    private OCRResultDTO ocrResult;
    private List<Double> embedding;
    private String embeddingContent;
    private String description;
    private String category;
    
    // ✅ ADDED: Additional fields for OCR disabled functionality
    private boolean ocrEnabled;
    private boolean embeddingGenerated;
    private String processingMessage;
}


