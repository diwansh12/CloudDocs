package com.clouddocs.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ✅ FIXED: Create ObjectMapper with Java 8 time support
        ObjectMapper objectMapper = createObjectMapperWithJavaTimeSupport();
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
        
        // Portfolio-optimized cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
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
        
        // ✅ FIXED: Use ObjectMapper with Java 8 time support
        ObjectMapper objectMapper = createObjectMapperWithJavaTimeSupport();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
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
     * ✅ FIXED: Create ObjectMapper with proper Java 8 date/time support
     */
    private ObjectMapper createObjectMapperWithJavaTimeSupport() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // ✅ Register JavaTimeModule for Java 8 date/time support
        objectMapper.registerModule(new JavaTimeModule());
        
        // ✅ Disable writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configure visibility
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // Use LaissezFaireSubTypeValidator for type safety
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        return objectMapper;
    }
}
