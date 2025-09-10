package com.clouddocs.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/search")  // ✅ No /api needed due to context-path
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    @PostMapping("/semantic")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> semanticSearch(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            logger.info("🔍 Semantic search for: {}", query);
            
            // Return empty results for now (AI feature coming soon)
            Map<String, Object> response = new HashMap<>();
            response.put("results", new ArrayList<>());
            response.put("query", query);
            response.put("count", 0);
            response.put("message", "Semantic search coming soon");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Semantic search failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Semantic search temporarily unavailable");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("results", new ArrayList<>());
            errorResponse.put("count", 0);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Search Controller");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
