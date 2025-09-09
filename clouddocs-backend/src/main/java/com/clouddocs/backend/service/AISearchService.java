package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.dto.DocumentDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/**
 * 🤖 AI-powered semantic search service with multi-provider support
 */
@Service
public class AISearchService {
    
    private static final Logger log = LoggerFactory.getLogger(AISearchService.class);
    
    @Autowired
    private MultiProviderAIService multiProviderAIService;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private AIEmbeddingService embeddingService;
    
    /**
     * 🔍 Perform semantic search with multi-provider AI support
     */
    public List<DocumentDTO> semanticSearch(String query, String username, int limit) {
        log.info("🔍 Performing semantic search for query: '{}'", query);
        
        try {
            // ✅ UPDATED: Use multi-provider service for embedding generation
            List<Double> queryEmbedding = multiProviderAIService.generateEmbedding(query);
            log.debug("✅ Query embedding generated successfully with {} dimensions", queryEmbedding.size());
            
            // Get all documents with embeddings for this user
            List<Document> documentsWithEmbeddings = documentRepository
                .findByUploadedByUsernameAndEmbeddingGeneratedTrue(username);
            
            log.debug("📄 Found {} documents with embeddings for user: {}", 
                documentsWithEmbeddings.size(), username);
            
            if (documentsWithEmbeddings.isEmpty()) {
                log.info("ℹ️ No documents with embeddings found for user: {}", username);
                return List.of();
            }
            
            // Calculate similarity scores and sort
            List<DocumentWithScore> scoredDocuments = documentsWithEmbeddings.stream()
                .map(doc -> {
                    try {
                        List<Double> docEmbedding = embeddingService.jsonToEmbedding(doc.getEmbedding());
                        double similarity = embeddingService.calculateSimilarity(queryEmbedding, docEmbedding);
                        return new DocumentWithScore(doc, similarity);
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to calculate similarity for document {}: {}", 
                            doc.getId(), e.getMessage());
                        return new DocumentWithScore(doc, 0.0);
                    }
                })
                .filter(scored -> scored.score > 0.65)
                .sorted(Comparator.comparing(DocumentWithScore::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
            
            log.info("✅ Semantic search completed: {} relevant documents found (threshold: 0.65)", 
                scoredDocuments.size());
            
            // Convert to DTOs and return
            return scoredDocuments.stream()
                .map(scored -> {
                    DocumentDTO dto = documentService.convertToDTO(scored.document);
                    dto.setAiScore(scored.score);
                    return dto;
                })
                .collect(Collectors.toList());
                
        } catch (EmbeddingException e) {
            log.error("❌ Multi-provider embedding generation failed: {} (Provider: {})", 
                e.getMessage(), e.getProviderName());
            throw new RuntimeException("AI search temporarily unavailable: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("❌ Semantic search failed unexpectedly: {}", e.getMessage(), e);
            throw new RuntimeException("Semantic search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 🎯 Advanced hybrid search combining semantic + keyword matching
     */
    public List<DocumentDTO> hybridSearch(String query, String username, int limit) {
        log.info("🔄 Performing hybrid search for query: '{}'", query);
        
        try {
            // ✅ FIXED: Proper lambda syntax for CompletableFuture.supplyAsync
            CompletableFuture<List<DocumentDTO>> semanticResults = CompletableFuture
                .supplyAsync(() -> semanticSearch(query, username, limit));
            
            CompletableFuture<List<DocumentDTO>> keywordResults = CompletableFuture
                .supplyAsync(() -> performKeywordSearch(query, username, limit));
            
            // Wait for both results
            List<DocumentDTO> semanticDocs = semanticResults.join();
            List<DocumentDTO> keywordDocs = keywordResults.join();
            
            // Merge and boost documents found by both methods
            return mergeAndRankResults(semanticDocs, keywordDocs, limit);
            
        } catch (Exception e) {
            log.warn("⚠️ Hybrid search failed, falling back to keyword search: {}", e.getMessage());
            return performKeywordSearch(query, username, limit);
        }
    }
    
    /**
     * 🔍 Perform keyword-based search (fallback method)
     */
    private List<DocumentDTO> performKeywordSearch(String query, String username, int limit) {
        try {
            // Use existing document service search functionality
            List<Document> documents = documentRepository.findByUploadedByUsernameAndOriginalFilenameContainingIgnoreCase(
                username, query);
            
            return documents.stream()
                .limit(limit)
                .map(doc -> {
                    DocumentDTO dto = documentService.convertToDTO(doc);
                    dto.setAiScore(0.5); // Default score for keyword matches
                    dto.setSearchType("keyword");
                    return dto;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("❌ Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 📊 Generate embeddings for documents that don't have them
     */
    @Transactional
    public void generateMissingEmbeddings(String username) {
        try {
            List<Document> documentsWithoutEmbeddings = documentRepository
                .findByUploadedByUsernameAndEmbeddingGeneratedFalse(username);
            
            log.info("📊 Generating embeddings for {} documents using multi-provider AI", 
                documentsWithoutEmbeddings.size());
            
            if (documentsWithoutEmbeddings.isEmpty()) {
                log.info("✅ All documents already have embeddings for user: {}", username);
                return;
            }
            
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "Unknown";
            log.info("🤖 Using AI provider: {}", activeProvider);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Document doc : documentsWithoutEmbeddings) {
                try {
                    String content = createEmbeddingContent(doc);
                    log.debug("🔄 Processing document: {} (length: {} chars)", 
                        doc.getOriginalFilename(), content.length());
                    
                    List<Double> embedding = multiProviderAIService.generateEmbedding(content);
                    
                    doc.setEmbedding(embeddingService.embeddingToJson(embedding));
                    doc.setEmbeddingGenerated(true);
                    documentRepository.save(doc);
                    
                    successCount++;
                    log.debug("✅ Generated embedding for document: {} ({} dimensions)", 
                        doc.getOriginalFilename(), embedding.size());
                    
                    Thread.sleep(500);
                    
                } catch (EmbeddingException e) {
                    failureCount++;
                    log.error("❌ Failed to generate embedding for document {} using {}: {}", 
                        doc.getOriginalFilename(), e.getProviderName(), e.getMessage());
                    
                    if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                        log.error("🚫 Authentication error - stopping embedding generation");
                        break;
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("❌ Unexpected error generating embedding for document {}: {}", 
                        doc.getOriginalFilename(), e.getMessage());
                }
            }
            
            log.info("📊 Embedding generation completed: {} success, {} failures", 
                successCount, failureCount);
                
        } catch (Exception e) {
            log.error("💥 Critical error in generateMissingEmbeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }
    
    private String createEmbeddingContent(Document doc) {
        StringBuilder content = new StringBuilder();
        
        if (doc.getOriginalFilename() != null) {
            String filename = doc.getOriginalFilename().replaceAll("\\.[^.]+$", "");
            content.append("Document: ").append(filename.replace("_", " ").replace("-", " ")).append(". ");
        }
        
        if (doc.getDescription() != null && !doc.getDescription().trim().isEmpty()) {
            content.append("Description: ").append(doc.getDescription().trim()).append(". ");
        }
        
        if (doc.getCategory() != null && !doc.getCategory().trim().isEmpty()) {
            content.append("Category: ").append(doc.getCategory().trim()).append(". ");
        }
        
        if (doc.getDocumentType() != null) {
            content.append("Type: ").append(doc.getDocumentType()).append(" document. ");
        }
        
        String result = content.toString().trim();
        if (result.length() < 10) {
            result = "Document file: " + (doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "untitled");
        }
        
        return result;
    }
    
    /**
     * ✅ FIXED: Merge results method with proper implementation
     */
    private List<DocumentDTO> mergeAndRankResults(List<DocumentDTO> semanticResults, 
                                                 List<DocumentDTO> keywordResults, 
                                                 int limit) {
        Map<Long, DocumentDTO> documentMap = new HashMap<>();
        
        // Add semantic results
        semanticResults.forEach(doc -> {
            doc.setSearchType("semantic");
            documentMap.put(doc.getId(), doc);
        });
        
        // Add keyword results, boosting documents found by both methods
        keywordResults.forEach(doc -> {
            if (documentMap.containsKey(doc.getId())) {
                DocumentDTO existing = documentMap.get(doc.getId());
                existing.setAiScore(existing.getAiScore() != null ? existing.getAiScore() * 1.2 : 1.0);
                existing.setSearchType("hybrid");
            } else {
                doc.setSearchType("keyword");
                doc.setAiScore(0.5);
                documentMap.put(doc.getId(), doc);
            }
        });
        
        return documentMap.values().stream()
            .sorted((a, b) -> {
                Double scoreA = a.getAiScore() != null ? a.getAiScore() : 0.0;
                Double scoreB = b.getAiScore() != null ? b.getAiScore() : 0.0;
                return Double.compare(scoreB, scoreA);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * ✅ FIXED: Statistics method with proper return type
     */
    public Map<String, Object> getSearchStatistics(String username) {
        try {
            long totalDocuments = documentRepository.countByUploadedByUsername(username);
            long documentsWithEmbeddings = documentRepository.countByUploadedByUsernameAndEmbeddingGeneratedTrue(username);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDocuments", totalDocuments);
            stats.put("documentsWithEmbeddings", documentsWithEmbeddings);
            stats.put("embeddingCoverage", totalDocuments > 0 ? (double) documentsWithEmbeddings / totalDocuments : 0.0);
            stats.put("activeProvider", multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "None");
            stats.put("providersStatus", multiProviderAIService.getProvidersStatus());
            
            return stats;
        } catch (Exception e) {
            log.error("❌ Failed to get search statistics: {}", e.getMessage());
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Failed to get statistics: " + e.getMessage());
            return errorStats;
        }
    }
    
    private static class DocumentWithScore {
        final Document document;
        final double score;
        
        DocumentWithScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }
        
        public double getScore() { 
            return score; 
        }
    }
}
