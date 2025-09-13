package com.clouddocs.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * 🤖 Cohere Embedding Provider Implementation
 * Free tier: 1000 API calls per month
 */
@Component
@ConditionalOnProperty(name = "ai.providers.cohere.enabled", havingValue = "true")
public class CohereProvider implements EmbeddingProvider {
    
    private static final Logger log = LoggerFactory.getLogger(CohereProvider.class);
    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/embed";
    
    @Value("${ai.providers.cohere.api-key:}")
    private String apiKey;
    
    @Value("${ai.providers.cohere.priority:2}")
    private int priority;
    
    @Value("${ai.providers.cohere.model:embed-english-v3.0}")
    private String model;
    
    private WebClient webClient;
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void initialize() {
        this.webClient = WebClient.builder()
            .baseUrl(COHERE_API_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
            
        this.objectMapper = new ObjectMapper();
        
        log.info("🤖 Cohere provider initialized with model: {}", model);
    }
    
    @Override
    public List<Double> generateEmbedding(String text) throws EmbeddingException {
        try {
            log.debug("🤖 Cohere generating embedding for text length: {}", text.length());
            
            // Build request payload
            Map<String, Object> requestBody = Map.of(
                "texts", List.of(text),
                "model", model,
                "input_type", "search_document"
            );
            
            // Make API call
            String response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            return parseEmbeddingResponse(response);
            
        } catch (WebClientResponseException ex) {
            log.error("❌ Cohere HTTP error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new EmbeddingException("Cohere", "Rate limit exceeded", 429, ex);
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new EmbeddingException("Cohere", "Invalid API key", 401, ex);
            } else {
                throw new EmbeddingException("Cohere", 
                    "HTTP error: " + ex.getMessage(), 
                    ex.getStatusCode().value(), ex);
            }
            
        } catch (Exception ex) {
            log.error("💥 Cohere unexpected error: {}", ex.getMessage(), ex);
            throw new EmbeddingException("Cohere", "Unexpected error: " + ex.getMessage(), ex);
        }
    }
    
    private List<Double> parseEmbeddingResponse(String response) throws EmbeddingException {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode embeddingsNode = rootNode.get("embeddings");
            
            if (embeddingsNode == null || !embeddingsNode.isArray() || embeddingsNode.size() == 0) {
                throw new EmbeddingException("Cohere", "Invalid response format - no embeddings found");
            }
            
            JsonNode firstEmbedding = embeddingsNode.get(0);
            if (!firstEmbedding.isArray()) {
                throw new EmbeddingException("Cohere", "Invalid embedding format");
            }
            
            List<Double> embedding = StreamSupport.stream(firstEmbedding.spliterator(), false)
                .map(JsonNode::asDouble)
                .toList();
                
            log.debug("✅ Cohere embedding generated successfully: {} dimensions", embedding.size());
            return embedding;
            
        } catch (Exception ex) {
            throw new EmbeddingException("Cohere", "Failed to parse response: " + ex.getMessage(), ex);
        }
    }
    
    @Override
    public String getProviderName() {
        return "Cohere";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.debug("Cohere not available - no API key configured");
                return false;
            }
            
            return true;
            
        } catch (Exception ex) {
            log.warn("Cohere availability check failed: {}", ex.getMessage());
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
            "model", model,
            "hasApiKey", apiKey != null && !apiKey.trim().isEmpty(),
            "freeQuota", "1000 calls/month"
        );
    }
    
    @Override
    public int getEmbeddingDimensions() {
        return 1024; // Cohere embed-english-v3.0 dimensions
    }
}
