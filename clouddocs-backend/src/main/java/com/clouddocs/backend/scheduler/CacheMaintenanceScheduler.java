package com.clouddocs.backend.scheduler;

import com.clouddocs.backend.service.SmartCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheMaintenanceScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheMaintenanceScheduler.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private SmartCachingService cachingService;
    
    // Clear dashboard stats cache every 10 minutes
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void clearDashboardStatsCache() {
        try {
            cachingService.evictAllDashboardStats();
            logger.info("🧹 Scheduled: Dashboard stats cache cleared");
        } catch (Exception e) {
            logger.error("❌ Failed to clear dashboard stats cache: {}", e.getMessage());
        }
    }
    
    // Clear all caches at 2 AM daily (optional)
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyCacheClear() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                if (!cacheName.equals("ocr-results") && !cacheName.equals("ai-classifications")) {
                    cacheManager.getCache(cacheName).clear();
                }
            });
            logger.info("🧹 Scheduled: Daily cache maintenance completed");
        } catch (Exception e) {
            logger.error("❌ Daily cache maintenance failed: {}", e.getMessage());
        }
    }
}
