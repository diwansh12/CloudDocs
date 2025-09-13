package com.clouddocs.backend.controller;

import com.clouddocs.backend.service.SmartCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/actuator/health")
public class ProductionHealthController {
    
    @Autowired
    private SmartCachingService cachingService;
    
    @GetMapping("/cache")
    public ResponseEntity<?> cacheHealth() {
        Map<String, Object> health = cachingService.getCacheHealth();
        
        boolean isHealthy = "UP".equals(health.get("redis")) && 
                           "AVAILABLE".equals(health.get("cacheManager"));
        
        return ResponseEntity.status(isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
            .body(health);
    }
    
    @GetMapping("/system")
    public ResponseEntity<?> systemHealth() {
        Map<String, Object> system = Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis(),
            "version", "2.0.0",
            "environment", "portfolio"
        );
        
        return ResponseEntity.ok(system);
    }
}
