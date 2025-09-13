package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.SmartCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/admin/cache")
@PreAuthorize("hasRole('ADMIN')")
public class CacheManagementController {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private SmartCachingService cachingService;
    
    @GetMapping("/stats")
    public ResponseEntity<?> getCacheStatistics() {
        Map<String, Object> stats = cachingService.getCacheStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @DeleteMapping("/evict-all")
    public ResponseEntity<?> evictAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            cacheManager.getCache(cacheName).clear();
        });
        
        return ResponseEntity.ok(Map.of(
            "message", "All caches evicted successfully",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @DeleteMapping("/evict/{cacheName}")
    public ResponseEntity<?> evictSpecificCache(@PathVariable String cacheName) {
        try {
            cacheManager.getCache(cacheName).clear();
            return ResponseEntity.ok(Map.of(
                "message", "Cache '" + cacheName + "' evicted successfully",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to evict cache: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/warm-up")
    public ResponseEntity<?> warmUpCaches() {
        // Pre-load frequently accessed data
        try {
            // Warm up document cache with recent documents
            // Warm up user cache with active users
            // Implementation specific to your needs
            
            return ResponseEntity.ok(Map.of(
                "message", "Cache warm-up initiated",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cache warm-up failed: " + e.getMessage()
            ));
        }
    }
}
