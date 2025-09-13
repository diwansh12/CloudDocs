package com.clouddocs.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class AdvancedCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ✅ FIXED: Use GenericJackson2JsonRedisSerializer to avoid deprecated methods
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .computePrefixWith(cacheName -> "clouddocs:cache:" + cacheName + ":")
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(jsonSerializer))
            .disableCachingNullValues();
        
        // Specific cache configurations with different TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Documents cache - 2 hours (frequently accessed)
        cacheConfigurations.put("documents", defaultConfig
            .entryTtl(Duration.ofHours(2)));
            
        // User cache - 1 hour
        cacheConfigurations.put("users", defaultConfig
            .entryTtl(Duration.ofHours(1)));
            
        // Workflows cache - 15 minutes (changes frequently)
        cacheConfigurations.put("workflows", defaultConfig
            .entryTtl(Duration.ofMinutes(15)));
            
        // OCR results cache - 24 hours (expensive to recompute)
        cacheConfigurations.put("ocr-results", defaultConfig
            .entryTtl(Duration.ofHours(24)));
            
        // AI classifications cache - 7 days (rarely changes)
        cacheConfigurations.put("ai-classifications", defaultConfig
            .entryTtl(Duration.ofDays(7)));
            
        // Dashboard stats cache - 10 minutes
        cacheConfigurations.put("dashboard-stats", defaultConfig
            .entryTtl(Duration.ofMinutes(10)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // ✅ FIXED: Use GenericJackson2JsonRedisSerializer with proper ObjectMapper configuration
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // String serialization for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        return template;
    }
    
    /**
     * ✅ FIXED: Create properly configured ObjectMapper without deprecated methods
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Configure visibility
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // ✅ FIXED: Use LaissezFaireSubTypeValidator instead of deprecated activateDefaultTyping
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        return objectMapper;
    }
    
    /**
     * ✅ NEW: Alternative configuration method using Jackson2JsonRedisSerializer with constructor
     */
    private Jackson2JsonRedisSerializer<Object> createJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = createObjectMapper();
        
        // ✅ FIXED: Use constructor instead of deprecated setObjectMapper
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
    
    /**
     * ✅ NEW: Cache configuration for high-performance scenarios
     */
    @Bean("highPerformanceCacheManager")
    public CacheManager highPerformanceCacheManager(RedisConnectionFactory connectionFactory) {
        // Use faster but less type-safe serialization for high-performance scenarios
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .computePrefixWith(cacheName -> "clouddocs:hp:" + cacheName + ":")
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
