package com.clouddocs.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class AIEmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIEmbeddingService.class);
    
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;
    
    public AIEmbeddingService(OpenAIService openAIService, ObjectMapper objectMapper) {
        this.openAIService = openAIService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate embedding for text
     */
    public List<Double> generateEmbedding(String text) {
        return openAIService.generateEmbedding(text);
    }
    
    /**
     * Convert embedding to JSON string for database storage
     */
    public String embeddingToJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert embedding to JSON", e);
        }
    }
    
    /**
     * Convert JSON string back to embedding list
     */
    @SuppressWarnings("unchecked")
    public List<Double> jsonToEmbedding(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to embedding", e);
        }
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    public double calculateSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1.size() != embedding2.size()) {
            throw new IllegalArgumentException("Embeddings must have same dimensions");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            dotProduct += embedding1.get(i) * embedding2.get(i);
            norm1 += Math.pow(embedding1.get(i), 2);
            norm2 += Math.pow(embedding2.get(i), 2);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
