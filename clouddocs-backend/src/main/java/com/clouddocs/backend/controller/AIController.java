package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.FeatureFlagService;
import com.clouddocs.backend.service.AISearchService;
import com.clouddocs.backend.service.MultiProviderAIService;
import com.clouddocs.backend.dto.DocumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🤖 AI-powered search controller with multi-provider support
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class AIController {
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    @Autowired
    private AISearchService aiSearchService;
    
    @Autowired
    private MultiProviderAIService multiProviderAIService;
    
    /**
     * 🔍 AI-powered semantic search with multi-provider support
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> semanticSearch(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String username = userDetails.getUsername();
            
            // Check if AI search is enabled for this user
            if (!featureFlagService.isAiSearchEnabledForUser(username)) {
                return ResponseEntity.ok(Map.of(
                    "message", "AI search is not available for your account",
                    "fallback", "Please use regular search",
                    "username", username
                ));
            }
            
            // Validate request
            String query = (String) request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Query parameter is required"
                ));
            }
            
            if (query.trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Query must be at least 2 characters long"
                ));
            }
            
            int limit = (int) request.getOrDefault("limit", 10);
            boolean useHybrid = (boolean) request.getOrDefault("hybrid", false);
            
            // Perform search
            List<DocumentDTO> results = useHybrid 
                ? aiSearchService.hybridSearch(query.trim(), username, limit)
                : aiSearchService.semanticSearch(query.trim(), username, limit);
            
            // Get active provider info
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "None";
            
            return ResponseEntity.ok(Map.of(
                "query", query.trim(),
                "results", results,
                "count", results.size(),
                "type", useHybrid ? "ai_hybrid_search" : "ai_semantic_search",
                "activeProvider", activeProvider,
                "beta_user", true,
                "searchMethod", useHybrid ? "hybrid" : "semantic"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "AI search failed: " + e.getMessage(),
                "fallback", "Please try regular search",
                "type", "error"
            ));
        }
    }
    
    /**
     * 🎯 Generate embeddings with multi-provider support
     */
    @PostMapping("/generate-embeddings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> generateEmbeddings(@AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String username = userDetails.getUsername();
            
            // Check if embedding generation is enabled for this user
            if (!featureFlagService.isAiEmbeddingEnabledForUser(username)) {
                return ResponseEntity.ok(Map.of(
                    "message", "AI embedding generation is not available for your account",
                    "username", username
                ));
            }
            
            // Get active provider info
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "None";
            
            if ("None".equals(activeProvider)) {
                return ResponseEntity.status(503).body(Map.of(
                    "error", "No AI providers available",
                    "message", "Please try again later"
                ));
            }
            
            // Generate embeddings
            aiSearchService.generateMissingEmbeddings(username);
            
            return ResponseEntity.ok(Map.of(
                "message", "Embeddings generated successfully",
                "status", "completed",
                "activeProvider", activeProvider,
                "beta_user", true,
                "username", username
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to generate embeddings: " + e.getMessage(),
                "message", "Please try again later"
            ));
        }
    }
    
    /**
     * 📊 User-specific AI status with provider information
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserAiStatus(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails.getUsername();
            
            // Get provider status
            Map<String, Object> providersStatus = multiProviderAIService.getProvidersStatus();
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "None";
            
            // Get user's document statistics
            Map<String, Object> searchStats = aiSearchService.getSearchStatistics(username);
            
            return ResponseEntity.ok(Map.of(
                "username", username,
                // User-specific AI feature flags
                "aiSearchEnabled", featureFlagService.isAiSearchEnabledForUser(username),
                "aiEmbeddingEnabled", featureFlagService.isAiEmbeddingEnabledForUser(username),
                "aiChatEnabled", featureFlagService.isAiChatEnabledForUser(username),
                
                // Global flags for debugging
                "globalFlags", Map.of(
                    "searchGlobal", featureFlagService.isAiSearchEnabled(),
                    "embeddingGlobal", featureFlagService.isAiEmbeddingEnabled(),
                    "chatGlobal", featureFlagService.isAiChatEnabled()
                ),
                
                // Provider information
                "activeProvider", activeProvider,
                "providersStatus", providersStatus,
                
                // User statistics
                "searchStatistics", searchStats,
                
                // Beta user information
                "betaUsers", featureFlagService.getBetaUsers(),
                "isBetaUser", featureFlagService.getBetaUsers().contains(username)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get AI status: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 🤖 AI providers status and health check
     */
    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProvidersStatus(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> status = multiProviderAIService.getProvidersStatus();
            String activeProvider = multiProviderAIService.getActiveProvider() != null 
                ? multiProviderAIService.getActiveProvider().getProviderName() 
                : "None";
            
            return ResponseEntity.ok(Map.of(
                "username", userDetails.getUsername(),
                "providersStatus", status,
                "activeProvider", activeProvider,
                "timestamp", System.currentTimeMillis(),
                "systemHealth", Map.of(
                    "hasActiveProvider", !"None".equals(activeProvider),
                    "totalProviders", status.get("totalProviders"),
                    "availableProviders", status.get("availableProviders")
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get providers status: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 🎯 Advanced hybrid search endpoint
     */
    @PostMapping("/hybrid-search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> hybridSearch(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String username = userDetails.getUsername();
            
            if (!featureFlagService.isAiSearchEnabledForUser(username)) {
                return ResponseEntity.ok(Map.of(
                    "message", "AI hybrid search is not available for your account",
                    "fallback", "Please use regular search"
                ));
            }
            
            String query = (String) request.get("query");
            if (query == null || query.trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Query must be at least 2 characters long"
                ));
            }
            
            int limit = (int) request.getOrDefault("limit", 10);
            
            List<DocumentDTO> results = aiSearchService.hybridSearch(query.trim(), username, limit);
            
            return ResponseEntity.ok(Map.of(
                "query", query.trim(),
                "results", results,
                "count", results.size(),
                "type", "ai_hybrid_search",
                "activeProvider", multiProviderAIService.getActiveProvider() != null 
                    ? multiProviderAIService.getActiveProvider().getProviderName() 
                    : "None",
                "searchMethod", "hybrid"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Hybrid search failed: " + e.getMessage(),
                "fallback", "Please try regular search"
            ));
        }
    }
}
