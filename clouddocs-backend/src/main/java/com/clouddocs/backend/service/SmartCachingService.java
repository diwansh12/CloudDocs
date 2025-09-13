package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.DocumentDTO;
import com.clouddocs.backend.dto.workflow.WorkflowInstanceDTO;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@CacheConfig(cacheNames = "smart-cache")
public class SmartCachingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartCachingService.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private UserRepository userRepository;
    
    // ===== DOCUMENT CACHING =====
    
    @Cacheable(value = "documents", key = "#id", unless = "#result == null")
    public DocumentDTO getCachedDocument(Long id) {
        logger.info("🔍 Cache miss for document {}, fetching from database", id);
        return documentService.getDocumentById(id);
    }
    
    /**
     * ✅ FIXED: Updated method signature and call to include all required parameters
     */
    @Cacheable(value = "documents", key = "'user:' + #userId + ':page:' + #page + ':size:' + #size + ':sort:' + #sortBy + ':dir:' + #sortDir")
    public List<DocumentDTO> getCachedUserDocuments(Long userId, int page, int size, String sortBy, String sortDir) {
        logger.info("🔍 Cache miss for user {} documents, fetching from database", userId);
        return documentService.getMyDocuments(page, size, sortBy, sortDir).getContent();
    }
    
    /**
     * ✅ ADDITIONAL: Overloaded method with default sorting for backward compatibility
     */
    @Cacheable(value = "documents", key = "'user:' + #userId + ':page:' + #page + ':size:' + #size + ':default'")
    public List<DocumentDTO> getCachedUserDocuments(Long userId, int page, int size) {
        logger.info("🔍 Cache miss for user {} documents with default sorting, fetching from database", userId);
        return documentService.getMyDocuments(page, size, "uploadDate", "desc").getContent();
    }
    
    @CacheEvict(value = "documents", key = "#id")
    public void evictDocumentCache(Long id) {
        logger.info("🗑️ Evicting document {} from cache", id);
    }
    
    @CacheEvict(value = "documents", allEntries = true)
    public void evictAllDocuments() {
        logger.info("🗑️ Evicting all documents from cache");
    }
    
    /**
     * ✅ NEW: Evict user-specific document cache
     */
    public void evictUserDocuments(Long userId) {
        try {
            String keyPattern = "clouddocs:cache:documents::user:" + userId + ":*";
            redisTemplate.delete(redisTemplate.keys(keyPattern));
            logger.info("🗑️ Evicted user {} document caches", userId);
        } catch (Exception e) {
            logger.warn("⚠️ Failed to evict user document caches: {}", e.getMessage());
        }
    }
    
    // ===== WORKFLOW CACHING =====
    
    @Cacheable(value = "workflows", key = "#id", unless = "#result == null")
    public WorkflowInstanceDTO getCachedWorkflow(Long id) {
        logger.info("🔍 Cache miss for workflow {}, fetching from database", id);
        return workflowService.getWorkflowById(id);
    }
    
    @CacheEvict(value = "workflows", key = "#id")
    public void evictWorkflowCache(Long id) {
        logger.info("🗑️ Evicting workflow {} from cache", id);
    }
    
    // ===== USER CACHING =====
    
   
    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    public User getCachedUser(String username) {
        logger.info("🔍 Cache miss for user {}, fetching from database", username);
        try {
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            logger.error("❌ Failed to fetch user from database: {}", e.getMessage());
            return null;
        }
    }
    
    // ===== OCR RESULTS CACHING =====
    
    @Cacheable(value = "ocr-results", key = "#fileHash")
    public String getCachedOCRResult(String fileHash) {
        logger.info("🔍 Cache miss for OCR file hash {}", fileHash);
        return null; // Return null if not cached, will trigger OCR processing
    }
    
    @CachePut(value = "ocr-results", key = "#fileHash")
    public String cacheOCRResult(String fileHash, String ocrText) {
        logger.info("💾 Caching OCR result for file hash {}", fileHash);
        return ocrText;
    }
    
    // ===== AI CLASSIFICATION CACHING =====
    
    @Cacheable(value = "ai-classifications", key = "#contentHash")
    public Map<String, Object> getCachedAIClassification(String contentHash) {
        logger.info("🤖 Cache miss for AI classification {}", contentHash);
        return null;
    }
    
    @CachePut(value = "ai-classifications", key = "#contentHash")
    public Map<String, Object> cacheAIClassification(String contentHash, Map<String, Object> classification) {
        logger.info("🤖 Caching AI classification for content hash {}", contentHash);
        return classification;
    }
    
    // ===== DASHBOARD STATS CACHING =====
    
   
    @Cacheable(value = "dashboard-stats", key = "'stats:' + #userId")
    public Map<String, Object> getCachedDashboardStats(Long userId) {
        logger.info("📊 Cache miss for dashboard stats for user {}", userId);
        try {
            // Get basic document statistics for the user
            Map<String, Object> stats = new HashMap<>();
            
            // Use existing document service methods to build stats
            long totalDocs = documentService.getAllDocuments(0, Integer.MAX_VALUE, "uploadDate", "desc", null, null, null).getTotalElements();
            long userDocs = documentService.getMyDocuments(0, Integer.MAX_VALUE, "uploadDate", "desc").getTotalElements();
            
            stats.put("totalDocuments", userDocs);
            stats.put("systemTotalDocuments", totalDocs);
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("userId", userId);
            
            return stats;
        } catch (Exception e) {
            logger.error("❌ Failed to fetch dashboard stats: {}", e.getMessage());
            return Map.of("error", "Failed to fetch stats", "timestamp", System.currentTimeMillis());
        }
    }
    
    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public void evictAllDashboardStats() {
        logger.info("📊 Evicting all dashboard stats from cache");
    }
    
    // ===== CUSTOM CACHING METHODS =====
    
    public void cacheWithCustomTTL(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set("clouddocs:custom:" + key, value, timeout, unit);
        logger.info("💾 Cached {} with custom TTL: {} {}", key, timeout, unit);
    }
    
    public Object getCustomCache(String key) {
        Object value = redisTemplate.opsForValue().get("clouddocs:custom:" + key);
        if (value != null) {
            logger.info("✅ Cache hit for custom key: {}", key);
        } else {
            logger.info("❌ Cache miss for custom key: {}", key);
        }
        return value;
    }
    
    public void evictCustomCache(String key) {
        redisTemplate.delete("clouddocs:custom:" + key);
        logger.info("🗑️ Evicted custom cache key: {}", key);
    }
    
    /**
     * ✅ NEW: Batch cache operations for performance
     */
    public void batchEvictDocuments(List<Long> documentIds) {
        for (Long id : documentIds) {
            evictDocumentCache(id);
        }
        logger.info("🗑️ Batch evicted {} document caches", documentIds.size());
    }
    
    /**
     * ✅ NEW: Cache preloading for frequently accessed documents
     */
    public void preloadFrequentDocuments(List<Long> documentIds) {
        logger.info("🔥 Preloading {} frequent documents into cache", documentIds.size());
        for (Long id : documentIds) {
            try {
                getCachedDocument(id);
            } catch (Exception e) {
                logger.warn("⚠️ Failed to preload document {}: {}", id, e.getMessage());
            }
        }
    }
    
    // ===== CACHE STATISTICS =====
    
    public Map<String, Object> getCacheStatistics() {
        try {
            Map<String, Object> stats = Map.of(
                "cacheNames", cacheManager.getCacheNames(),
                "totalCaches", cacheManager.getCacheNames().size(),
                "redisConnected", isRedisConnected(),
                "timestamp", System.currentTimeMillis()
            );
            
            logger.info("📈 Cache statistics requested: {}", stats);
            return stats;
        } catch (Exception e) {
            logger.error("❌ Failed to get cache statistics: {}", e.getMessage());
            return Map.of("error", "Failed to get cache statistics", "timestamp", System.currentTimeMillis());
        }
    }
    
    private boolean isRedisConnected() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.warn("⚠️ Redis connection check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ NEW: Cache health check
     */
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = Map.of(
            "redis", isRedisConnected() ? "UP" : "DOWN",
            "cacheManager", cacheManager != null ? "AVAILABLE" : "UNAVAILABLE",
            "cacheCount", cacheManager != null ? cacheManager.getCacheNames().size() : 0,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("🏥 Cache health check: {}", health);
        return health;
    }
}
