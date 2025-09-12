package com.clouddocs.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class FeatureFlagService {
    
    @Value("${ai.search.enabled:true}")  // ✅ Default to true for portfolio
    private boolean aiSearchEnabled;
    
    @Value("${ai.chat.enabled:true}")    // ✅ Default to true for portfolio
    private boolean aiChatEnabled;
    
    @Value("${ai.embedding.enabled:true}") // ✅ Default to true for portfolio
    private boolean aiEmbeddingEnabled;
    
    // ✅ Keep beta users for future features or admin testing
    @Value("${ai.beta.users:}")
    private String betaUsersString;
    
    // ✅ PORTFOLIO VERSION: Enable for ALL users
    public boolean isAiSearchEnabledForUser(String username) {
        return aiSearchEnabled; // ✅ Removed beta user check - available to ALL
    }
    
    public boolean isAiChatEnabledForUser(String username) {
        return aiChatEnabled; // ✅ Removed beta user check - available to ALL
    }
    
    public boolean isAiEmbeddingEnabledForUser(String username) {
        return aiEmbeddingEnabled; // ✅ Removed beta user check - available to ALL
    }
    
    // ✅ Global flag methods (for admin/monitoring)
    public boolean isAiSearchEnabled() {
        return aiSearchEnabled;
    }
    
    public boolean isAiChatEnabled() {
        return aiChatEnabled;
    }
    
    public boolean isAiEmbeddingEnabled() {
        return aiEmbeddingEnabled;
    }
    
    // ✅ Keep beta user methods for potential future use
    private boolean isBetaUser(String username) {
        if (betaUsersString == null || betaUsersString.trim().isEmpty()) {
            return false;
        }
        
        List<String> betaUsers = Arrays.asList(betaUsersString.split(","));
        return betaUsers.stream()
                .map(String::trim)
                .anyMatch(username::equals);
    }
    
    public List<String> getBetaUsers() {
        if (betaUsersString == null || betaUsersString.trim().isEmpty()) {
            return Arrays.asList();
        }
        return Arrays.asList(betaUsersString.split(","));
    }
    
    // ✅ NEW: Portfolio-friendly method to show all AI features are enabled
    public boolean areAllAiFeaturesEnabled() {
        return aiSearchEnabled && aiChatEnabled && aiEmbeddingEnabled;
    }
    
    // ✅ NEW: Get feature status for portfolio demo
    public String getPortfolioFeatureStatus() {
        return String.format(
            "🚀 AI Features Status: Search=%s, Chat=%s, Embeddings=%s - Available to ALL users",
            aiSearchEnabled ? "✅" : "❌",
            aiChatEnabled ? "✅" : "❌", 
            aiEmbeddingEnabled ? "✅" : "❌"
        );
    }
}
