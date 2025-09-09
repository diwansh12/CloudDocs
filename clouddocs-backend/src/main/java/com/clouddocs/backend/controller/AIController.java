package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.FeatureFlagService;
import com.clouddocs.backend.service.AISearchService;
import com.clouddocs.backend.dto.DocumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class AIController {
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    @Autowired
    private AISearchService aiSearchService;
    
    /**
     * ✅ UPDATED: AI-powered semantic search with beta user support
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> semanticSearch(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // ✅ CHANGED: Use user-specific feature flag check
        if (!featureFlagService.isAiSearchEnabledForUser(userDetails.getUsername())) {
            return ResponseEntity.ok(Map.of(
                "message", "AI search is not available for your account",
                "fallback", "Please use regular search"
            ));
        }
        
        try {
            String query = (String) request.get("query");
            int limit = (int) request.getOrDefault("limit", 10);
            
            List<DocumentDTO> results = aiSearchService.semanticSearch(
                query, userDetails.getUsername(), limit);
            
            return ResponseEntity.ok(Map.of(
                "query", query,
                "results", results,
                "count", results.size(),
                "type", "ai_semantic_search",
                "beta_user", true // ✅ NEW: Indicate this user has beta access
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "AI search failed: " + e.getMessage(),
                "fallback", "Please try regular search"
            ));
        }
    }
    
    /**
     * ✅ UPDATED: Generate embeddings with beta user support
     */
    @PostMapping("/generate-embeddings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> generateEmbeddings(@AuthenticationPrincipal UserDetails userDetails) {
        
        // ✅ CHANGED: Use user-specific feature flag check
        if (!featureFlagService.isAiEmbeddingEnabledForUser(userDetails.getUsername())) {
            return ResponseEntity.ok(Map.of(
                "message", "AI embedding generation is not available for your account"
            ));
        }
        
        try {
            aiSearchService.generateMissingEmbeddings(userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                "message", "Embeddings generated successfully",
                "status", "completed",
                "beta_user", true // ✅ NEW: Indicate this user has beta access
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to generate embeddings: " + e.getMessage()
            ));
        }
    }
    
    /**
     * ✅ UPDATED: User-specific AI status check
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserAiStatus(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        
        return ResponseEntity.ok(Map.of(
            "username", username,
            // ✅ CHANGED: Use user-specific methods
            "aiSearchEnabled", featureFlagService.isAiSearchEnabledForUser(username),
            "aiEmbeddingEnabled", featureFlagService.isAiEmbeddingEnabledForUser(username),
            "aiChatEnabled", featureFlagService.isAiChatEnabledForUser(username),
            // ✅ NEW: Also show global flags for debugging
            "globalFlags", Map.of(
                "searchGlobal", featureFlagService.isAiSearchEnabled(),
                "embeddingGlobal", featureFlagService.isAiEmbeddingEnabled(),
                "chatGlobal", featureFlagService.isAiChatEnabled()
            ),
            // ✅ NEW: Show beta users list (for debugging)
            "betaUsers", featureFlagService.getBetaUsers()
        ));
    }
}

