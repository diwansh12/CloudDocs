package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.dto.DocumentDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class AISearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(AISearchService.class);
    
    private final AIEmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    
    public AISearchService(AIEmbeddingService embeddingService, 
                          DocumentRepository documentRepository,
                          DocumentService documentService) {
        this.embeddingService = embeddingService;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }
    
    /**
     * Perform semantic search on documents
     */
    public List<DocumentDTO> semanticSearch(String query, String username, int limit) {
        logger.info("🔍 Performing semantic search for query: '{}'", query);
        
        try {
            // Generate embedding for search query
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
            
            // Get all documents with embeddings for this user
            List<Document> documentsWithEmbeddings = documentRepository
                .findByUploadedByUsernameAndEmbeddingGeneratedTrue(username);
            
            logger.debug("📄 Found {} documents with embeddings", documentsWithEmbeddings.size());
            
            // Calculate similarity scores and sort
            List<DocumentWithScore> scoredDocuments = documentsWithEmbeddings.stream()
                .map(doc -> {
                    List<Double> docEmbedding = embeddingService.jsonToEmbedding(doc.getEmbedding());
                    double similarity = embeddingService.calculateSimilarity(queryEmbedding, docEmbedding);
                    return new DocumentWithScore(doc, similarity);
                })
                .filter(scored -> scored.score > 0.7) // Only return relevant results
                .sorted(Comparator.comparing(DocumentWithScore::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
            
            logger.info("✅ Found {} relevant documents", scoredDocuments.size());
            
            // Convert to DTOs and return
            return scoredDocuments.stream()
                .map(scored -> {
                    DocumentDTO dto = documentService.convertToDTO(scored.document);
                    dto.setAiScore(scored.score); // Now this method exists!
                    return dto;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("❌ Semantic search failed: {}", e.getMessage());
            throw new RuntimeException("Semantic search failed", e);
        }
    }
    
    /**
     * Generate embeddings for documents that don't have them
     */
    @Transactional
    public void generateMissingEmbeddings(String username) {
        List<Document> documentsWithoutEmbeddings = documentRepository
            .findByUploadedByUsernameAndEmbeddingGeneratedFalse(username);
            
        logger.info("📊 Generating embeddings for {} documents", documentsWithoutEmbeddings.size());
        
        for (Document doc : documentsWithoutEmbeddings) {
            try {
                // Create text content for embedding
                String content = createEmbeddingContent(doc);
                
                // Generate embedding
                List<Double> embedding = embeddingService.generateEmbedding(content);
                
                // Save to database
                doc.setEmbedding(embeddingService.embeddingToJson(embedding));
                doc.setEmbeddingGenerated(true);
                documentRepository.save(doc);
                
                logger.debug("✅ Generated embedding for document: {}", doc.getOriginalFilename());
                
            } catch (Exception e) {
                logger.error("❌ Failed to generate embedding for document {}: {}", 
                           doc.getId(), e.getMessage());
            }
        }
    }
    
    private String createEmbeddingContent(Document doc) {
        // ✅ FIXED: Use correct method names from Document entity
        StringBuilder content = new StringBuilder();
        
        if (doc.getOriginalFilename() != null) {
            content.append("Title: ").append(doc.getOriginalFilename()).append(". ");
        }
        if (doc.getDescription() != null) {
            content.append("Description: ").append(doc.getDescription()).append(". ");
        }
        if (doc.getCategory() != null) {
            content.append("Category: ").append(doc.getCategory()).append(". ");
        }
        
        return content.toString();
    }
    
    // Helper class for scoring
    private static class DocumentWithScore {
        final Document document;
        final double score;
        
        DocumentWithScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }
        
        public double getScore() { return score; }
    }
}
