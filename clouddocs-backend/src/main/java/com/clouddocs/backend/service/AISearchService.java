package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.repository.DocumentRepository;
import com.clouddocs.backend.dto.DocumentDTO;
import com.clouddocs.backend.service.MultiProviderAIService;
import com.clouddocs.backend.service.EmbeddingException;
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
 * Features: OpenAI + Cohere providers, automatic failover, hybrid search, dimension safety
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
     * 🔍 Perform semantic search with multi-provider AI support and comprehensive debugging
     */
    public List<DocumentDTO> semanticSearch(String query, String username, int limit) {
        log.info("🔍 Performing semantic search for query: '{}', user: '{}'", query, username);
        
        try {
            // Generate query embedding with multi-provider support
            List<Double> queryEmbedding = multiProviderAIService.generateEmbedding(query);
            log.info("✅ Query embedding generated successfully with {} dimensions", queryEmbedding.size());
            
            // Get all documents with embeddings for this user
            List<Document> documentsWithEmbeddings = documentRepository
                .findByUploadedByUsernameAndEmbeddingGeneratedTrue(username);
            
            log.info("📄 Found {} documents with embeddings for user: {}", 
                documentsWithEmbeddings.size(), username);
            
            if (documentsWithEmbeddings.isEmpty()) {
                log.info("ℹ️ No documents with embeddings found for user: {}", username);
                return List.of();
            }
            
            // Calculate similarity scores with comprehensive logging and safety checks
            List<DocumentWithScore> allScoredDocuments = documentsWithEmbeddings.stream()
                .map(doc -> {
                    try {
                        // Get document embedding
                        List<Double> docEmbedding = embeddingService.jsonToEmbedding(doc.getEmbedding());
                        
                        // ✅ ENHANCED: Add dimension logging for debugging
                        log.debug("📊 Document '{}' - Query dims: {}, Doc dims: {}", 
                            doc.getOriginalFilename(), queryEmbedding.size(), docEmbedding.size());
                        
                        // ✅ FIXED: Use dimension-safe similarity calculation
                        double similarity = calculateSafeSimilarity(queryEmbedding, docEmbedding, doc.getOriginalFilename());
                        
                        // ✅ FIXED: Proper similarity logging with all parameters
                        log.info("🎯 SIMILARITY: Query '{}' → Document '{}' → Score: {:.4f}", 
                            query, doc.getOriginalFilename(), similarity);
                        
                        return new DocumentWithScore(doc, similarity);
                    } catch (Exception e) {
                        log.error("❌ Error calculating similarity for document '{}': {}", 
                            doc.getOriginalFilename(), e.getMessage());
                        return new DocumentWithScore(doc, 0.0);
                    }
                })
                .sorted(Comparator.comparing(DocumentWithScore::getScore).reversed())
                .collect(Collectors.toList());
            
            // ✅ ENHANCED: Log ALL similarity scores before filtering
            log.info("📊 ALL SIMILARITY SCORES (before filtering):");
            allScoredDocuments.forEach(scored -> 
                log.info("   - {}: {:.4f}", scored.document.getOriginalFilename(), scored.score)
            );
            
            // ✅ FIXED: Apply lower threshold for better recall
            List<DocumentWithScore> filteredResults = allScoredDocuments.stream()
                .filter(scored -> scored.score > 0.30) // ✅ LOWERED from 0.55 to 0.30
                .limit(limit)
                .collect(Collectors.toList());
            
            // ✅ FIXED: Consistent threshold logging
            log.info("✅ Semantic search completed: {} relevant documents found (threshold: 0.30)", 
                filteredResults.size());
            
            // Enhanced results logging
            if (!filteredResults.isEmpty()) {
                log.info("📊 FINAL RESULTS (above threshold 0.30):");
                filteredResults.forEach(scored -> 
                    log.info("   ✓ {}: {:.4f}", scored.document.getOriginalFilename(), scored.score)
                );
            } else {
                double highestScore = !allScoredDocuments.isEmpty() ? allScoredDocuments.get(0).score : 0.0;
                log.warn("⚠️ No documents passed the similarity threshold of 0.30");
                if (highestScore > 0.0) {
                    log.info("💡 Highest similarity score was: {:.4f} for '{}'", 
                        highestScore, allScoredDocuments.get(0).document.getOriginalFilename());
                    if (highestScore > 0.15) {
                        log.info("💡 Consider lowering threshold to {:.2f} to include more results", 
                            Math.max(0.15, highestScore - 0.05));
                    }
                }
            }
            
            // Convert to DTOs and return
            return filteredResults.stream()
                .map(scored -> {
                    DocumentDTO dto = documentService.convertToDTO(scored.document);
                    dto.setAiScore(scored.score);
                    dto.setSearchType("semantic");
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
     * ✅ ENHANCED: Dimension safety check with detailed logging
     */
    private double calculateSafeSimilarity(List<Double> queryEmbedding, List<Double> docEmbedding, String docName) {
        if (queryEmbedding.size() != docEmbedding.size()) {
            log.warn("⚠️ DIMENSION MISMATCH: Document '{}' - Query: {} dims, Document: {} dims", 
                docName, queryEmbedding.size(), docEmbedding.size());
            log.warn("💡 This indicates embeddings were generated by different AI providers");
            log.warn("🔧 Consider running regenerate-embeddings to fix dimension mismatches");
            return 0.0; // Return 0 similarity for mismatched dimensions
        }
        
        try {
            double similarity = embeddingService.calculateSimilarity(queryEmbedding, docEmbedding);
            log.debug("✅ Similarity calculated successfully for '{}': {:.4f}", docName, similarity);
            return similarity;
        } catch (Exception e) {
            log.error("❌ Error in similarity calculation for '{}': {}", docName, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 🎯 Advanced hybrid search combining semantic + keyword matching
     */
    public List<DocumentDTO> hybridSearch(String query, String username, int limit) {
        log.info("🔄 Performing hybrid search for query: '{}'", query);
        
        try {
            // Execute semantic and keyword searches in parallel
            CompletableFuture<List<DocumentDTO>> semanticResults = CompletableFuture
                .supplyAsync(() -> semanticSearch(query, username, limit));
            
            CompletableFuture<List<DocumentDTO>> keywordResults = CompletableFuture
                .supplyAsync(() -> performKeywordSearch(query, username, limit));
            
            // Wait for both results
            List<DocumentDTO> semanticDocs = semanticResults.join();
            List<DocumentDTO> keywordDocs = keywordResults.join();
            
            log.info("🔄 Hybrid search results - Semantic: {}, Keyword: {}", 
                semanticDocs.size(), keywordDocs.size());
            
            // Merge and boost documents found by both methods
            return mergeAndRankResults(semanticDocs, keywordDocs, limit);
            
        } catch (Exception e) {
            log.warn("⚠️ Hybrid search failed, falling back to keyword search: {}", e.getMessage());
            return performKeywordSearch(query, username, limit);
        }
    }
    
    /**
     * 🔍 Enhanced keyword-based search with multiple field matching
     */
    private List<DocumentDTO> performKeywordSearch(String query, String username, int limit) {
        try {
            // Search across multiple fields for better recall
            List<Document> documents = documentRepository
                .findByUploadedByUsernameAndOriginalFilenameContainingIgnoreCase(username, query);
            
            log.debug("🔍 Keyword search found {} documents for query: '{}'", documents.size(), query);
            
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
            
            // Get active provider info for logging
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "Unknown";
            log.info("🤖 Using AI provider: {}", activeProvider);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Document doc : documentsWithoutEmbeddings) {
                try {
                    // Create enriched content for embedding
                    String content = createEmbeddingContent(doc);
                    log.debug("🔄 Processing document: {} (content length: {} chars)", 
                        doc.getOriginalFilename(), content.length());
                    
                    // Generate embedding with current active provider
                    List<Double> embedding = multiProviderAIService.generateEmbedding(content);
                    
                    // Store embedding
                    doc.setEmbedding(embeddingService.embeddingToJson(embedding));
                    doc.setEmbeddingGenerated(true);
                    documentRepository.save(doc);
                    
                    successCount++;
                    log.debug("✅ Generated embedding for document: {} ({} dimensions)", 
                        doc.getOriginalFilename(), embedding.size());
                    
                    // Rate limiting delay
                    Thread.sleep(500);
                    
                } catch (EmbeddingException e) {
                    failureCount++;
                    log.error("❌ Failed to generate embedding for document {} using {}: {}", 
                        doc.getOriginalFilename(), e.getProviderName(), e.getMessage());
                    
                    // Stop on authentication errors
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
    
    /**
     * ✅ ENHANCED: Create enriched embedding content with semantic expansion
     */
    private String createEmbeddingContent(Document doc) {
        StringBuilder content = new StringBuilder();
        
        // Document filename with semantic expansion
        if (doc.getOriginalFilename() != null) {
            String filename = doc.getOriginalFilename().replaceAll("\\.[^.]+$", "");
            content.append("Document: ").append(filename.replace("_", " ").replace("-", " ")).append(". ");
            
            // Add semantic terms based on filename
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.contains("voter")) {
                content.append("Voting document. Election identification. Citizen ID card. Electoral registration. ");
            }
            if (lowerFilename.contains("addhaar") || lowerFilename.contains("aadhaar")) {
                content.append("National identity card. Government ID. Citizen identification. Official identity document. ");
            }
            if (lowerFilename.contains("resume") || lowerFilename.contains("cv")) {
                content.append("Professional resume. Career document. Employment history. Skills profile. ");
            }
        }
        
        // Description with context
        if (doc.getDescription() != null && !doc.getDescription().trim().isEmpty()) {
            content.append("Description: ").append(doc.getDescription().trim()).append(". ");
            
            // Add semantic expansion based on description keywords
            String lowerDesc = doc.getDescription().toLowerCase();
            if (lowerDesc.contains("id") || lowerDesc.contains("identity")) {
                content.append("Personal identification document. Identity verification. Official ID card. ");
            }
        }
        
        // Category with extensive semantic expansion  
        if (doc.getCategory() != null && !doc.getCategory().trim().isEmpty()) {
            content.append("Category: ").append(doc.getCategory().trim()).append(". ");
            
            String category = doc.getCategory().toLowerCase();
            if (category.contains("national id") || category.contains("id")) {
                content.append("Identity document. Personal identification. Official government ID. ");
                content.append("National identity card. Citizen identification. State issued ID. ");
                content.append("Identity verification document. Personal identity proof. Government identification. ");
                content.append("Voter registration card. Electoral identification document. ");
            }
        }
        
        // Document type
        if (doc.getDocumentType() != null) {
            content.append("Type: ").append(doc.getDocumentType()).append(" document. ");
            content.append("Official document. Government paperwork. Personal records. ");
        }
        
        String result = content.toString().trim();
        if (result.length() < 20) {
            result = "Document: " + (doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "untitled document");
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
    
    /**
     * 🔄 Force regenerate ALL embeddings with current active provider
     */
    @Transactional
    public void forceRegenerateAllEmbeddings(String username) {
        log.info("🔄 Force regenerating ALL embeddings for user: {}", username);
        
        try {
            // Get ALL documents for this user (regardless of embedding status)
            List<Document> allDocuments = documentRepository.findByUploadedByUsername(username);
            
            log.info("📄 Regenerating embeddings for {} documents", allDocuments.size());
            
            if (allDocuments.isEmpty()) {
                log.info("ℹ️ No documents found for user: {}", username);
                return;
            }
            
            // Get active provider info
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "Unknown";
            log.info("🤖 Using AI provider for regeneration: {}", activeProvider);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Document doc : allDocuments) {
                try {
                    // Create enriched content for embedding
                    String content = createEmbeddingContent(doc);
                    log.debug("🔄 Processing document: {} (content length: {} chars)", 
                        doc.getOriginalFilename(), content.length());
                    
                    // ✅ Generate new embedding with current active provider
                    List<Double> embedding = multiProviderAIService.generateEmbedding(content);
                    
                    // Convert embedding to JSON and store
                    String embeddingJson = embeddingService.embeddingToJson(embedding);
                    doc.setEmbedding(embeddingJson);
                    doc.setEmbeddingGenerated(true);
                    
                    // Save updated document
                    documentRepository.save(doc);
                    
                    successCount++;
                    log.info("✅ Regenerated embedding for: {} ({} dimensions)", 
                        doc.getOriginalFilename(), embedding.size());
                    
                    // Rate limiting delay - 2 seconds between requests
                    Thread.sleep(2000);
                    
                } catch (EmbeddingException e) {
                    failureCount++;
                    log.error("❌ Failed to regenerate embedding for {} using {}: {}", 
                        doc.getOriginalFilename(), e.getProviderName(), e.getMessage());
                    
                    // Stop on authentication errors to prevent hammering
                    if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                        log.error("🚫 Authentication error - stopping regeneration");
                        break;
                    }
                    
                } catch (InterruptedException e) {
                    log.warn("⚠️ Thread interrupted during regeneration");
                    Thread.currentThread().interrupt();
                    break;
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("❌ Unexpected error regenerating embedding for {}: {}", 
                        doc.getOriginalFilename(), e.getMessage());
                }
            }
            
            log.info("🎯 Embedding regeneration completed: {} success, {} failures", 
                successCount, failureCount);
                
        } catch (Exception e) {
            log.error("💥 Critical error in forceRegenerateAllEmbeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to regenerate embeddings: " + e.getMessage(), e);
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
