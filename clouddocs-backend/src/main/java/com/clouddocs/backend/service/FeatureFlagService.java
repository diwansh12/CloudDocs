package com.clouddocs.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class FeatureFlagService {
    
    @Value("${ai.search.enabled:false}")
    private boolean aiSearchEnabled;
    
    @Value("${ai.chat.enabled:false}")
    private boolean aiChatEnabled;
    
    @Value("${ai.embedding.enabled:false}")
    private boolean aiEmbeddingEnabled;
    
    // ✅ NEW: Beta users configuration
    @Value("${ai.beta.users:}")
    private String betaUsersString;
    
    // ✅ EXISTING: Global flag methods (keep these)
    public boolean isAiSearchEnabled() {
        return aiSearchEnabled;
    }
    
    public boolean isAiChatEnabled() {
        return aiChatEnabled;
    }
    
    public boolean isAiEmbeddingEnabled() {
        return aiEmbeddingEnabled;
    }
    
    // ✅ NEW: User-specific methods for beta testing
    public boolean isAiSearchEnabledForUser(String username) {
        return aiSearchEnabled || isBetaUser(username);
    }
    
    public boolean isAiChatEnabledForUser(String username) {
        return aiChatEnabled || isBetaUser(username);
    }
    
    public boolean isAiEmbeddingEnabledForUser(String username) {
        return aiEmbeddingEnabled || isBetaUser(username);
    }
    
    // ✅ NEW: Check if user is in beta users list
    private boolean isBetaUser(String username) {
        if (betaUsersString == null || betaUsersString.trim().isEmpty()) {
            return false;
        }
        
        List<String> betaUsers = Arrays.asList(betaUsersString.split(","));
        return betaUsers.stream()
                .map(String::trim)
                .anyMatch(username::equals);
    }
    
    // ✅ NEW: Helper method to get beta users list (for debugging)
    public List<String> getBetaUsers() {
        if (betaUsersString == null || betaUsersString.trim().isEmpty()) {
            return Arrays.asList();
        }
        return Arrays.asList(betaUsersString.split(","));
    }
}
