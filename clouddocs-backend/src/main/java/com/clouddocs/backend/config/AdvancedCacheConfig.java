package com.clouddocs.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;  // ✅ ADD THIS IMPORT
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
    @Primary  // ✅ ADD THIS ANNOTATION
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
        
        // Portfolio-optimized cache configurations (shorter TTL for free tier)
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Optimized for 30MB Redis limit
        cacheConfigurations.put("documents", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("workflows", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("ocr-results", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("ai-classifications", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("dashboard-stats", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
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
    
    // ✅ KEEP: Secondary cache manager (no @Primary annotation)
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
