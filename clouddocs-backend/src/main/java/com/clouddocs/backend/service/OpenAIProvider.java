package com.clouddocs.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;

/**
 * 🤖 OpenAI Embedding Provider Implementation
 * Wraps existing OpenAI service with provider interface
 */
@Component
@ConditionalOnProperty(name = "ai.providers.openai.enabled", havingValue = "true", matchIfMissing = true)
public class OpenAIProvider implements EmbeddingProvider {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);
    
    @Autowired
    private OpenAIService openAIService;
    
    @Value("${ai.providers.openai.priority:1}")
    private int priority;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Override
    public List<Double> generateEmbedding(String text) throws EmbeddingException {
        try {
            log.debug("🤖 OpenAI generating embedding for text length: {}", text.length());
            
            List<Double> embedding = openAIService.generateEmbedding(text);
            
            log.debug("✅ OpenAI embedding generated successfully: {} dimensions", embedding.size());
            return embedding;
            
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("⏳ OpenAI rate limit exceeded: {}", ex.getMessage());
            throw new EmbeddingException("OpenAI", "Rate limit exceeded", 429, ex);
            
        } catch (HttpClientErrorException ex) {
            log.error("❌ OpenAI HTTP error: {} - {}", ex.getStatusCode(), ex.getMessage());
            throw new EmbeddingException("OpenAI", 
                "HTTP error: " + ex.getMessage(), 
                ex.getStatusCode().value(), ex);
                
        } catch (ResourceAccessException ex) {
            log.error("🌐 OpenAI network error: {}", ex.getMessage());
            throw new EmbeddingException("OpenAI", "Network error: " + ex.getMessage(), ex);
            
        } catch (Exception ex) {
            log.error("💥 OpenAI unexpected error: {}", ex.getMessage(), ex);
            throw new EmbeddingException("OpenAI", "Unexpected error: " + ex.getMessage(), ex);
        }
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Quick health check - API key exists and service is available
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.debug("OpenAI not available - no API key configured");
                return false;
            }
            
            // Could add a simple API call here for deeper health check
            // For now, assume available if API key exists
            return true;
            
        } catch (Exception ex) {
            log.warn("OpenAI availability check failed: {}", ex.getMessage());
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public Map<String, Object> getProviderInfo() {
        return Map.of(
            "name", getProviderName(),
            "available", isAvailable(),
            "priority", getPriority(),
            "dimensions", getEmbeddingDimensions(),
            "model", "text-embedding-3-small",
            "hasApiKey", apiKey != null && !apiKey.trim().isEmpty()
        );
    }
    
    @Override
    public int getEmbeddingDimensions() {
        return 1536; // OpenAI text-embedding-3-small dimensions
    }
}
