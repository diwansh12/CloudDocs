package com.clouddocs.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🤖 Multi-Provider AI Service
 * Orchestrates multiple embedding providers with automatic failover
 */
@Service
public class MultiProviderAIService {
    
    private static final Logger log = LoggerFactory.getLogger(MultiProviderAIService.class);
    
    @Autowired(required = false)
    private List<EmbeddingProvider> providers;
    
    private List<EmbeddingProvider> sortedProviders;
    
    @PostConstruct
    public void initialize() {
        if (providers == null || providers.isEmpty()) {
            log.warn("⚠️  No embedding providers found! AI features will be disabled.");
            return;
        }
        
        // Sort providers by priority (lower number = higher priority)
        sortedProviders = providers.stream()
            .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
            .collect(Collectors.toList());
        
        log.info("🤖 Multi-Provider AI Service initialized with {} providers:", sortedProviders.size());
        for (EmbeddingProvider provider : sortedProviders) {
            log.info("   - {} (priority: {}, available: {})", 
                provider.getProviderName(), 
                provider.getPriority(), 
                provider.isAvailable());
        }
    }
    
    /**
     * Generate embedding using the first available provider
     */
    public List<Double> generateEmbedding(String text) throws EmbeddingException {
        if (sortedProviders == null || sortedProviders.isEmpty()) {
            throw new EmbeddingException("MultiProvider", "No embedding providers configured");
        }
        
        EmbeddingException lastException = null;
        
        for (EmbeddingProvider provider : sortedProviders) {
            try {
                if (!provider.isAvailable()) {
                    log.debug("⏭️  Skipping unavailable provider: {}", provider.getProviderName());
                    continue;
                }
                
                log.debug("🚀 Attempting embedding with provider: {}", provider.getProviderName());
                List<Double> result = provider.generateEmbedding(text);
                
                log.info("✅ Embedding generated successfully using: {}", provider.getProviderName());
                return result;
                
            } catch (EmbeddingException ex) {
                lastException = ex;
                log.warn("❌ Provider {} failed: {} ({})", 
                    provider.getProviderName(), ex.getMessage(), ex.getStatusCode());
                
                // Don't retry on authentication errors
                if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
                    log.debug("🚫 Skipping retry for authentication error");
                    continue;
                }
                
                // For rate limits, continue to next provider
                if (ex.getStatusCode() == 429) {
                    log.debug("⏳ Rate limit hit, trying next provider");
                    continue;
                }
                
                // For other errors, continue to next provider
                continue;
            }
        }
        
        // All providers failed
        String errorMessage = lastException != null 
            ? "All embedding providers failed. Last error: " + lastException.getMessage()
            : "All embedding providers are unavailable";
            
        log.error("💥 {}", errorMessage);
        throw new EmbeddingException("MultiProvider", errorMessage, lastException);
    }
    
    /**
     * Get status of all providers
     */
    public Map<String, Object> getProvidersStatus() {
        if (sortedProviders == null) {
            return Map.of("providers", List.of(), "totalProviders", 0);
        }
        
        List<Map<String, Object>> providerInfos = sortedProviders.stream()
            .map(EmbeddingProvider::getProviderInfo)
            .collect(Collectors.toList());
        
        long availableCount = sortedProviders.stream()
            .mapToLong(p -> p.isAvailable() ? 1 : 0)
            .sum();
        
        return Map.of(
            "providers", providerInfos,
            "totalProviders", sortedProviders.size(),
            "availableProviders", availableCount,
            "hasAvailableProvider", availableCount > 0
        );
    }
    
    /**
     * Get the currently active (highest priority available) provider
     */
    public EmbeddingProvider getActiveProvider() {
        if (sortedProviders == null) return null;
        
        return sortedProviders.stream()
            .filter(EmbeddingProvider::isAvailable)
            .findFirst()
            .orElse(null);
    }
}
