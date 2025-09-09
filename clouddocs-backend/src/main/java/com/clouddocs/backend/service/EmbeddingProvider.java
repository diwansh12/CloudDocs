package com.clouddocs.backend.service;

import java.util.List;
import java.util.Map;

/**
 * 🤖 Abstraction for AI embedding providers
 * Enables multi-provider support with automatic failover
 */
public interface EmbeddingProvider {
    
    /**
     * Generate vector embedding for given text
     * @param text Input text to embed
     * @return List of doubles representing the embedding vector
     * @throws EmbeddingException if embedding generation fails
     */
    List<Double> generateEmbedding(String text) throws EmbeddingException;
    
    /**
     * Get the provider name for logging and identification
     * @return Provider name (e.g., "OpenAI", "Cohere", "HuggingFace")
     */
    String getProviderName();
    
    /**
     * Check if provider is currently available and healthy
     * @return true if provider can generate embeddings, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get provider priority (lower number = higher priority)
     * @return Priority order for provider selection
     */
    default int getPriority() {
        return 100; // Default priority
    }
    
    /**
     * Get provider configuration and status info
     * @return Map containing provider metadata
     */
    default Map<String, Object> getProviderInfo() {
        return Map.of(
            "name", getProviderName(),
            "available", isAvailable(),
            "priority", getPriority()
        );
    }
    
    /**
     * Get expected embedding dimensions for this provider
     * @return Number of dimensions in embedding vectors
     */
    default int getEmbeddingDimensions() {
        return 1536; // Default for most models
    }
}
