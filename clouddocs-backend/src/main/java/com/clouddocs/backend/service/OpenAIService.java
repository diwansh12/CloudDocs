package com.clouddocs.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    public OpenAIService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();
    }
    
    /**
     * Generate embeddings using OpenAI API
     */
    public List<Double> generateEmbedding(String text) {
        try {
            logger.debug("🤖 Generating embedding for text: {}", text.substring(0, Math.min(50, text.length())));
            
            Map<String, Object> request = Map.of(
                "input", text,
                "model", "text-embedding-3-small"
            );
            
            String response = webClient
                .post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response to get embedding
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode embeddingArray = jsonResponse.get("data").get(0).get("embedding");
            
            List<Double> embedding = objectMapper.convertValue(embeddingArray, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
            
            logger.debug("✅ Generated embedding with {} dimensions", embedding.size());
            return embedding;
            
        } catch (Exception e) {
            logger.error("❌ Failed to generate embedding: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    /**
     * Chat with GPT-3.5 (for document chat feature)
     */
    public String chatWithGPT(String prompt) {
        try {
            logger.debug("💬 Sending chat request to GPT");
            
            Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt
            );
            
            Map<String, Object> request = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(message),
                "max_tokens", 500,
                "temperature", 0.7
            );
            
            String response = webClient
                .post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String answer = jsonResponse.get("choices").get(0).get("message").get("content").asText();
            
            logger.debug("✅ Received chat response");
            return answer;
            
        } catch (Exception e) {
            logger.error("❌ Chat request failed: {}", e.getMessage());
            throw new RuntimeException("Chat request failed", e);
        }
    }
}
