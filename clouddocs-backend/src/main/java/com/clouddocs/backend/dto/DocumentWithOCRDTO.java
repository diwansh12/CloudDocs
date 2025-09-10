package com.clouddocs.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
@AllArgsConstructor
public class DocumentWithOCRDTO {
    private MultipartFile file;
    private OCRResultDTO ocrResult;
    private List<Double> embedding;
    private String embeddingContent;
    private String description;
    private String category;
}

